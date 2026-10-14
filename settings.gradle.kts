pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "contract-plugin"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
include("demo")
