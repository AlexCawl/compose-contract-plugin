plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()
}
