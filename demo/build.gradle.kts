plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()
}

dependencies {
    implementation(project(":plugin-annotations"))
    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
    testImplementation(libs.junit)
}
