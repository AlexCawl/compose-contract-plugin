package com.alexcawl.contract.services

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import com.alexcawl.contract.ContractPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
    configureAnnotations()
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = ContractPluginRegistrar()
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        // Consumer modules must resolve generated declarations from library metadata without the plugin.
        if (module.files.any { "@GenerateContract" in it.originalContent }) {
            with(registrar) { registerExtensions(configuration) }
        }
    }
}
