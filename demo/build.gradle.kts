plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":plugin-annotations"))
    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
    testImplementation(libs.junit)
}
