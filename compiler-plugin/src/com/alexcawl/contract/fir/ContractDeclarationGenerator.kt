@file:OptIn(
    org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi::class,
    org.jetbrains.kotlin.fir.symbols.SymbolInternals::class,
    org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess::class,
)

package com.alexcawl.contract.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.jvm.isJvm

internal val generateContractId = ClassId.topLevel(FqName("com.alexcawl.contract.GenerateContract"))
internal val contractPredicate = LookupPredicate.create { annotated(generateContractId.asSingleFqName()) }
internal val copyName = Name.identifier("copy")
internal fun implementationId(contract: ClassId): ClassId =
    ClassId(contract.packageFqName, Name.identifier("${contract.shortClassName}Impl"))

internal fun callbackName(name: Name): Name = Name.identifier("\$contract\$${name.asString()}")

internal fun FirRegularClass.contractMembers(): List<FirCallableDeclaration> =
    declarations.filterIsInstance<FirProperty>() + declarations.filterIsInstance<FirNamedFunction>()

internal fun FirRegularClass.unsupportedContractReason(): String? = when {
    classKind != ClassKind.INTERFACE -> "@GenerateContract requires an interface"
    symbol.classId.isNestedClass || isLocal -> "Contracts must be top-level interfaces"
    typeParameters.isNotEmpty() -> "Generic contracts are not supported"
    status.isExpect || status.isExternal || status.modality == Modality.SEALED ->
        "Expect, external, and sealed contracts are not supported"
    status.visibility !in setOf(Visibilities.Public, Visibilities.Internal) -> "Contracts must be public or internal"
    superTypeRefs.any { it.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId != StandardClassIds.Any } ->
        "Contract inheritance is not supported"
    contractMembers().any { it is FirProperty && it.isVar } -> "Contracts can declare val properties, but not var"
    contractMembers().any {
        it.receiverParameter != null || it.contextParameters.isNotEmpty() || it.typeParameters.isNotEmpty()
    } ->
        "Generic, extension, and context members are not supported"
    contractMembers().any { it.status.visibility != Visibilities.Public || it.status.isSuspend } ->
        "Contract members must be public and non-suspend"
    contractMembers().map { it.symbol.name }.let { it.size != it.toSet().size } ->
        "Contract members must have distinct names (overloads are not supported)"
    contractMembers().any { it.symbol.name.asString() in setOf("equals", "hashCode", "copy") } ->
        "Contract members cannot be named equals, hashCode, or copy"
    else -> null
}

class ContractDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(contractPredicate)
    }

    private fun contracts(): List<FirRegularClassSymbol> =
        session.predicateBasedProvider.getSymbolsByPredicate(contractPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .filter {
                it.fir.classKind == ClassKind.INTERFACE && !it.classId.isNestedClass &&
                    !it.fir.isLocal && it.typeParameterSymbols.isEmpty()
            }

    private fun contractFor(owner: FirClassSymbol<*>): FirRegularClassSymbol? =
        contracts().firstOrNull { implementationId(it.classId) == owner.classId }

    override fun getTopLevelClassIds(): Set<ClassId> = contracts().mapTo(mutableSetOf()) { implementationId(it.classId) }

    override fun getTopLevelCallableIds(): Set<CallableId> = contracts().flatMapTo(mutableSetOf()) {
        listOf(CallableId(it.classId.packageFqName, it.name), CallableId(it.classId.packageFqName, copyName))
    }

    override fun hasPackage(packageFqName: FqName): Boolean = contracts().any { it.classId.packageFqName == packageFqName }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val contract = contracts().firstOrNull { implementationId(it.classId) == classId } ?: return null
        return createTopLevelClass(classId, Key) {
            visibility = contract.fir.status.visibility
            superType(contract.defaultType())
        }.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val contract = contractFor(classSymbol) ?: return emptySet()
        return buildSet {
            add(SpecialNames.INIT)
            add(Name.identifier("equals"))
            add(Name.identifier("hashCode"))
            for (member in contract.fir.contractMembers()) {
                add(member.symbol.name)
                if (member is FirNamedFunction) add(callbackName(member.name))
            }
        }
    }

    private fun memberType(member: FirCallableDeclaration): ConeKotlinType {
        val returnType = member.symbol.resolvedReturnType
        return if (member is FirNamedFunction) {
            StandardClassIds.FunctionN(member.valueParameters.size).createConeType(
                session,
                (member.valueParameters.map { it.symbol.resolvedReturnType } + returnType).toTypedArray(),
            )
        } else returnType
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val contract = contractFor(context.owner) ?: return emptyList()
        return listOf(createConstructor(context.owner, Key, isPrimary = true) {
            visibility = contract.fir.status.visibility
            for (member in contract.fir.contractMembers()) valueParameter(member.symbol.name, memberType(member))
        }.symbol)
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val contract = contractFor(owner) ?: return emptyList()
        val member = contract.fir.contractMembers().firstOrNull {
            (if (it is FirNamedFunction) callbackName(it.name) else it.symbol.name) == callableId.callableName
        } ?: return emptyList()
        return listOf(createMemberProperty(owner, Key, callableId.callableName, memberType(member)) {
            if (member is FirNamedFunction) visibility = Visibilities.Private
            else status { isOverride = true }
        }.symbol)
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) {
            return contracts().filter {
                it.classId.packageFqName == callableId.packageName &&
                    (callableId.callableName == it.name || callableId.callableName == copyName)
            }.map { contract ->
                val isCopy = callableId.callableName == copyName
                createTopLevelFunction(Key, callableId, contract.defaultType(), containingFileName = "${contract.name}Contract") {
                    visibility = contract.fir.status.visibility
                    if (isCopy) extensionReceiverType(contract.defaultType())
                    for (member in contract.fir.contractMembers()) {
                        valueParameter(
                            member.symbol.name, memberType(member),
                            hasDefaultValue = isCopy || member is FirNamedFunction && member.body != null,
                        )
                    }
                }.also { function ->
                    if (session.moduleData.platform.isJvm()) {
                        function.replaceAnnotations(listOf(buildAnnotation {
                            annotationTypeRef = ClassId.topLevel(FqName("kotlin.jvm.JvmOverloads"))
                                .createConeType(session).toFirResolvedTypeRef()
                            argumentMapping = FirEmptyAnnotationArgumentMapping
                        }))
                    }
                }.symbol
            }
        }
        val owner = context.owner
        val contract = contractFor(owner) ?: return emptyList()
        val name = callableId.callableName
        val function = when (name.asString()) {
            "equals" -> createMemberFunction(owner, Key, name, session.builtinTypes.booleanType.coneType) {
                status { isOverride = true }
                valueParameter(Name.identifier("other"), session.builtinTypes.nullableAnyType.coneType)
            }
            "hashCode" -> createMemberFunction(owner, Key, name, session.builtinTypes.intType.coneType) {
                status { isOverride = true }
            }
            else -> {
                val member = contract.fir.contractMembers().filterIsInstance<FirNamedFunction>().firstOrNull { it.name == name }
                    ?: return emptyList()
                createMemberFunction(owner, Key, name, member.symbol.resolvedReturnType) {
                    status { isOverride = true }
                    for (parameter in member.valueParameters) {
                        valueParameter(parameter.name, parameter.symbol.resolvedReturnType, isVararg = parameter.isVararg)
                    }
                }
            }
        }
        return listOf(function.symbol)
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String = "Key"
    }
}
