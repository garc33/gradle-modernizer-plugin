package fr.herman.gradle.plugins.modernizer

import groovy.util.logging.Log
import org.gaul.modernizer_maven_plugin.Modernizer
import org.gaul.modernizer_maven_plugin.Violation
import org.gradle.api.Plugin
import org.gradle.api.Project

@Log
class ModernizerPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'java'
        project.extensions.create("modernizer", ModernizerPluginExtension)

        project.task('modernizer')<<{
            Map<String, Violation> violations
            InputStream is
            try {
                if (project.modernizer.violationsFile) {
                    is = new FileInputStream(new File(project.modernizer.violationsFile))
                } else {
                    is = ModernizerPlugin.class.getResourceAsStream('/modernizer.xml')
                }
                violations = Modernizer.parseFromXml(is)
            } catch (Throwable ioe) {
                throw new Exception('Error reading violation file', ioe)
            } finally {
                is?.close()
            }

            def exclusions = new HashSet<String>()
            if (project.modernizer.exclusionsFile) {
                is = null
                try {
                    File file = new File(project.modernizer.exclusionsFile)
                    if (file.exists()) {
                        is = new FileInputStream(file)
                    } else {
                        is = this.getClass().getClassLoader().getResourceAsStream(
                                project.modernizer.exclusionsFile);
                    }
                    if (is == null) {
                        throw new Exception("Could not find exclusion file: ${project.modernizer.exclusionsFile}")
                    }

                    exclusions.addAll(is.readLines())
                } catch (IOException ioe) {
                    throw new Exception("Error reading exclusion file: ${project.modernizer.exclusionsFile}", ioe)
                } finally {
                    is?.close()
                }
            }
            log.info("run modernizer on java ${project.targetCompatibility.toString()}")
            def modernizer = new Modernizer(project.targetCompatibility.toString(), violations, exclusions);

            try {
                Long count = recurseFiles(project,modernizer, project.sourceSets.main.output.classesDir)
                if (project.modernizer.includeTestClasses) {
                    count += recurseFiles(project,modernizer, project.sourceSets.test.output.classesDir)
                }
                if(0==count){
                    println 'No violation found'
                }else if (project.modernizer.failOnViolations) {
                    throw new Exception("Found ${count} violations")
                } else {
                    log.warning "Found ${count} violations"
                }
            } catch (IOException ioe) {
                throw new Exception("Error reading Java classes", ioe)
            }
        }
    }


    def recurseFiles(Project project, Modernizer modernizer, File file) throws IOException {
        long count = 0;
        if (!file.exists()) {
            return count;
        }
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    count += recurseFiles(project,modernizer, new File(file, child));
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            InputStream is = new FileInputStream(file);
            try {
                modernizer.check(is).each {
                    String name = file.getPath();
                    //                    if (name.startsWith(project.sourceSets.main.output.classesDir.getPath())) {
                    //                        name = project.sourceSets.main.java.getPath() + name.substring(
                    //                                project.sourceSets.main.output.classesDir.getPath().length());
                    //                        name = name.substring(0,
                    //                                name.length() - ".class".length()) + ".java";
                    //                    } else if (name.startsWith(project.sourceSets.test.output.classesDir.getPath())) {
                    //                        name = testSourceDirectory.getPath() + name.substring(
                    //                                project.sourceSets.test.output.classesDir.getPath().length());
                    //                        name = name.substring(0,
                    //                                name.length() - ".class".length()) + ".java";
                    //                    }

                    log.warning(name + ":" +
                            it.getLineNumber() + ": " +
                            it.getViolation().getComment());
                    ++count;
                }
            } finally {
                is?.close()
            }
        }
        return count;
    }
}