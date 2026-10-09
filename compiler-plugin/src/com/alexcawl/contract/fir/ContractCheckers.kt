@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package com.alexcawl.contract.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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
        val members = declaration.contractMembers()
        val composableId = ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))
        for ((index, member) in members.withIndex()) {
            if (member.hasAnnotation(composableId, context.session) ||
                member is FirProperty && member.getter?.hasAnnotation(composableId, context.session) == true
            ) {
                reporter.reportOn(member.source, FirErrors.UNSUPPORTED, "Contract members cannot be @Composable")
            }
            if (member !is FirNamedFunction) continue
            val supportedReceivers = mutableSetOf<FirThisReceiverExpression>()
            val visitor = object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    if (element is FirQualifiedAccessExpression) {
                        val receiver = element.dispatchReceiver as? FirThisReceiverExpression
                        if (receiver?.calleeReference?.boundSymbol == declaration.symbol) {
                            val referencedIndex = members.indexOfFirst { it.symbol == element.calleeReference.toResolvedCallableSymbol() }
                            if (referencedIndex in 0 until index &&
                                !(element is FirCallableReferenceAccess && members[referencedIndex] is FirProperty)
                            ) supportedReceivers += receiver
                        }
                    }
                    if (element is FirThisReceiverExpression && element.calleeReference.boundSymbol == declaration.symbol &&
                        element !in supportedReceivers
                    ) {
                        reporter.reportOn(
                            element.source ?: member.source, FirErrors.UNSUPPORTED,
                            "Default contract methods may only read properties and use earlier methods; standalone this, property references, and recursive/forward calls are not supported",
                        )
                    }
                    element.acceptChildren(this)
                }
            }
            member.body?.accept(visitor)
            member.valueParameters.forEach { it.defaultValue?.accept(visitor) }
        }
    }
}
