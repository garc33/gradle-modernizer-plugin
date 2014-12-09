gradle-modernizer-plugin
========================

Gradle wrapper for modernizer-maven-plugin

How to use it ?
===============
Add to your gradle script
```groovy
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath "com.github.garc33.gradle.plugins:gradle-modernizer-plugin:1.0" }
}
apply plugin: 'modernizer'
```
finally:

     gradle modernizer

Configuration
=============

| Settings                  | Description                                           |Default            |
|---------------------------|-------------------------------------------------------|-------------------|
|includeTestClasses         |Check tests classes                                    | false             |
|failOnViolations           |Fail build when violation occurs                       | false             |
|excludeNotInSources        |Exclude classes from packaged dependency               | false             |

