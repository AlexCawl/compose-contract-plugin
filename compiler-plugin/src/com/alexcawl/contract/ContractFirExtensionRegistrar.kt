package com.alexcawl.contract

import com.alexcawl.contract.fir.ContractCheckers
import com.alexcawl.contract.fir.ContractDeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class ContractFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::ContractDeclarationGenerator
        +::ContractCheckers
    }
}
