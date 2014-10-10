package fr.herman.gradle.plugins.modernizer

class ModernizerPluginExtension {
    String exclusionsFile
    String violationsFile
    boolean includeTestClasses = false
    boolean failOnViolations = false
    boolean excludeNotInSources = false
}
