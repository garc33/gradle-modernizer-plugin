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

        def modernizerTask = project.task('modernizer')<<{
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
            def modernizer = new Modernizer(project.targetCompatibility.toString(), violations, exclusions)

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

        modernizerTask.group = 'Verification'
        modernizerTask.description = 'Detects use of legacy APIs which modern Java versions supersede'
        modernizerTask.dependsOn('classes')
        project.tasks['check'].dependsOn('modernizer')
        project.configure(project) {
            afterEvaluate {
                if(project.modernizer.includeTestClasses)
                    modernizerTask.dependsOn('testClasses')
            }
        }
    }


    def recurseFiles(Project project, Modernizer modernizer, File file) throws IOException {
        long count = 0;
        if (!file.exists()) {
            return count
        }
        if (file.isDirectory()) {
            def children = file.list()
            if (children != null) {
                for (String child : children) {
                    count += recurseFiles(project,modernizer, new File(file, child));
                }
            }
        } else if (file.getPath().endsWith(".class")) {
            def is = new FileInputStream(file)
            try {
                modernizer.check(is).each {
                    String name = sourceFileName(project.sourceSets.main.java.getSrcDirs(), project.sourceSets.main.output.classesDir, file.path)
                    if(project.modernizer.includeTestClasses && name.endsWith('.class'))
                        name = sourceFileName(project.sourceSets.test.java.getSrcDirs(),project.sourceSets.test.output.classesDir, name)
                    if(!project.modernizer.excludeNotInSources || (it.getLineNumber()!=-1&&name.endsWith('.java'))){
                        log.warning(name + ':' + it.getLineNumber() + ': ' + it.getViolation().getComment())
                        ++count
                    }
                }
            } finally {
                is?.close()
            }
        }
        return count
    }

    def sourceFileName(Collection<File> sourcesDirs,File outputDir,String name){
        if(name.startsWith(outputDir.path)){
            for(File sourceDir : sourcesDirs){
                def newname = sourceDir.path + name.substring(outputDir.path.length())
                newname = newname.replace('.class', '.java')
                if(new File(newname).file)
                    return newname
            }
        }
        return name
    }
}
