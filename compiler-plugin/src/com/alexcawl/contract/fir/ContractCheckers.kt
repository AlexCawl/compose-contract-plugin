@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package com.alexcawl.contract.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ContractCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers = setOf(ContractChecker)
    }
}

private object ContractChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotation = declaration.getAnnotationByClassId(generateContractId, context.session) ?: return
        declaration.unsupportedContractReason()?.let { reason ->
            reporter.reportOn(annotation.source, FirErrors.UNSUPPORTED, reason)
            return
        }
        val methods = declaration.contractMembers()
        val getters = methods.map { JvmAbi.getterName(it.name.asString()) }
        if (getters.size != getters.toSet().size || getters.any { it == "getClass" }) {
            reporter.reportOn(annotation.source, FirErrors.UNSUPPORTED, "Contract callback getters must have distinct JVM names")
        }
        // copy$default also needs the receiver, masks, and marker within the JVM's 255 slots.
        if (methods.size + (methods.size + 31) / 32 + 2 > 255) {
            reporter.reportOn(annotation.source, FirErrors.UNSUPPORTED, "Contract callbacks exceed the JVM parameter-slot limit")
        }
        val contractId = declaration.symbol.classId
        val generatedIds = listOf(containerId(contractId), implementationId(contractId)) +
            listOf("ConstructorKt", "CopyKt").map { ClassId(contractId.packageFqName, Name.identifier("${contractId.shortClassName}$it")) }
        for (id in generatedIds) {
            val existing = context.session.symbolProvider.getClassLikeSymbolByClassId(id)
            if (existing != null && (existing.fir.origin as? FirDeclarationOrigin.Plugin)?.key != ContractDeclarationGenerator.Key) {
                reporter.reportOn(annotation.source, FirErrors.UNSUPPORTED, "Generated class ${id.relativeClassName} conflicts with an existing declaration")
            }
        }
        val composableId = ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))
        for (method in methods) {
            if (method.hasAnnotation(composableId, context.session)) {
                reporter.reportOn(method.source, FirErrors.UNSUPPORTED, "Contract members cannot be @Composable")
            }
        }
    }
}
