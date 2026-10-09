package com.alexcawl.contract.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

fun TestConfigurationBuilder.configureAnnotations() {
    useConfigurators(::PluginAnnotationsProvider)
    useCustomRuntimeClasspathProviders(::PluginAnnotationsClasspathProvider)
}

private class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(annotationsJvmRuntimeClasspath)
    }
}

private class PluginAnnotationsClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> = annotationsJvmRuntimeClasspath
}

private val annotationsJvmRuntimeClasspath = classpathFiles("annotationsRuntime.jvm.classpath")

private fun classpathFiles(property: String): List<File> {
    val property = System.getProperty(property)
        ?: error("Unable to get a valid classpath from '$property' property")
    return property.split(File.pathSeparator).map(::File)
}
