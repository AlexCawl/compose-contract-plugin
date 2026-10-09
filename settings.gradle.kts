pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "compose-contract-plugin"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
include("demo-jvm")
include("demo-android")
