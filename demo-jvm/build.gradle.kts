plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":plugin-annotations"))
    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
    testImplementation(libs.junit)
}

application {
    mainClass.set("com.alexcawl.contract.demo.jvm.MainKt")
}
