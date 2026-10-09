plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.buildconfig) apply false
}

val kotlinVersion = libs.versions.kotlin.get()
val pluginVersion = providers.gradleProperty("version").get()
require(pluginVersion.matches(Regex("${Regex.escape(kotlinVersion)}-[1-9][0-9]*"))) {
    "Plugin version '$pluginVersion' must be $kotlinVersion-<positive revision>"
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = pluginVersion
}

subprojects {
    pluginManager.withPlugin("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/alexcawl/compose-contract-plugin")
                    credentials {
                        username = providers.environmentVariable("GITHUB_ACTOR").orNull
                        password = providers.environmentVariable("GITHUB_TOKEN").orNull
                    }
                }
            }
        }
    }
}
