package fr.herman.gradle.plugins.modernizer

class ModernizerPluginExtension {
    def String exclusionsFile
    def String violationsFile
    def boolean includeTestClasses = false
    def boolean failOnViolations = false
}
