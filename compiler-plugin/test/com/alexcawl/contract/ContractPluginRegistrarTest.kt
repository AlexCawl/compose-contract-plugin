package com.alexcawl.contract

import com.alexcawl.contract.ir.ContractIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContractPluginRegistrarTest {
    @Test
    fun registersFirAndIrExtensionsOnce() {
        val storage = CompilerPluginRegistrar.ExtensionStorage()
        with(ContractPluginRegistrar()) {
            storage.registerExtensions(CompilerConfiguration.create())
        }

        val extensions = storage.registeredExtensions
        assertEquals(setOf(FirExtensionRegistrarAdapter, IrGenerationExtension), extensions.keys)
        assertIs<ContractFirExtensionRegistrar>(extensions.getValue(FirExtensionRegistrarAdapter).single())
        assertIs<ContractIrGenerationExtension>(extensions.getValue(IrGenerationExtension).single())
    }
}
