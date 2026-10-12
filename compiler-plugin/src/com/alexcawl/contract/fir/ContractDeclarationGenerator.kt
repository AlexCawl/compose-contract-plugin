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
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.ConstantValueKind

internal val generateContractId = ClassId.topLevel(FqName("com.alexcawl.contract.GenerateContract"))
internal val contractPredicate = LookupPredicate.create { annotated(generateContractId.asSingleFqName()) }
internal val copyName = Name.identifier("copy")
internal val contractName = Name.identifier("contract")
internal fun implementationId(contract: ClassId): ClassId =
    ClassId(contract.packageFqName, Name.identifier("${contract.shortClassName}Impl"))
internal fun containerId(contract: ClassId): ClassId =
    ClassId(contract.packageFqName, Name.identifier("${contract.shortClassName}Contract"))
internal fun callbackName(name: Name): Name = Name.identifier("\$contract\$${name.asString()}")

internal fun FirRegularClass.sourceMembers(): List<FirCallableDeclaration> =
    declarations.filterIsInstance<FirCallableDeclaration>().filter {
        (it is FirProperty || it is FirNamedFunction) &&
            (it.origin as? FirDeclarationOrigin.Plugin)?.key != ContractDeclarationGenerator.Key
    }

internal fun FirRegularClass.contractMembers(): List<FirNamedFunction> = sourceMembers().filterIsInstance<FirNamedFunction>()
internal fun FirRegularClass.prefixLengths(): IntRange {
    val methods = contractMembers()
    return (methods.indexOfLast { it.body == null } + 1)..methods.size
}

internal fun FirRegularClass.unsupportedContractReason(): String? = when {
    classKind != ClassKind.INTERFACE -> "@GenerateContract requires an interface"
    symbol.classId.isNestedClass || isLocal -> "Contracts must be top-level interfaces"
    typeParameters.isNotEmpty() -> "Generic contracts are not supported"
    status.isExpect || status.isExternal || status.modality == Modality.SEALED ->
        "Expect, external, and sealed contracts are not supported"
    status.visibility !in setOf(Visibilities.Public, Visibilities.Internal) -> "Contracts must be public or internal"
    superTypeRefs.any { it.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId != StandardClassIds.Any } ->
        "Contract inheritance is not supported"
    sourceMembers().any { it is FirProperty } -> "Contracts can declare functions, but not val or var properties"
    contractMembers().any {
        it.receiverParameter != null || it.contextParameters.isNotEmpty() || it.typeParameters.isNotEmpty()
    } -> "Generic, extension, and context members are not supported"
    contractMembers().any { it.status.visibility != Visibilities.Public || it.status.isSuspend } ->
        "Contract members must be public and non-suspend"
    contractMembers().map { it.name }.let { it.size != it.toSet().size } ->
        "Contract members must have distinct names (overloads are not supported)"
    contractMembers().any { it.name.asString() in setOf("equals", "hashCode", "copy", "contract") } ->
        "Contract members cannot be named equals, hashCode, copy, or contract"
    else -> null
}

class ContractDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() = register(contractPredicate)

    private fun contracts(): List<FirRegularClassSymbol> =
        session.predicateBasedProvider.getSymbolsByPredicate(contractPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .filter {
                it.fir.classKind == ClassKind.INTERFACE && !it.classId.isNestedClass &&
                    !it.fir.isLocal && it.typeParameterSymbols.isEmpty()
            }

    private fun contractFor(owner: FirClassSymbol<*>): FirRegularClassSymbol? = contracts().firstOrNull {
        implementationId(it.classId) == owner.classId || containerId(it.classId) == owner.classId
    }

    override fun getTopLevelClassIds(): Set<ClassId> = contracts().flatMapTo(mutableSetOf()) {
        listOf(implementationId(it.classId), containerId(it.classId))
    }

    override fun getTopLevelCallableIds(): Set<CallableId> = contracts().flatMapTo(mutableSetOf()) {
        listOf(CallableId(it.classId.packageFqName, it.name), CallableId(it.classId.packageFqName, copyName))
    }

    override fun hasPackage(packageFqName: FqName): Boolean = contracts().any { it.classId.packageFqName == packageFqName }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val contract = contracts().firstOrNull {
            implementationId(it.classId) == classId || containerId(it.classId) == classId
        } ?: return null
        val implementation = classId == implementationId(contract.classId)
        return createTopLevelClass(classId, Key) {
            visibility = if (implementation) Visibilities.Private else contract.fir.status.visibility
            if (implementation) superType(contract.defaultType())
        }.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (contracts().any { it.classId == classSymbol.classId }) return setOf(contractName)
        val contract = contractFor(classSymbol) ?: return emptySet()
        val implementation = classSymbol.classId == implementationId(contract.classId)
        return buildSet {
            add(SpecialNames.INIT)
            if (implementation) {
                add(Name.identifier("equals"))
                add(Name.identifier("hashCode"))
                add(contractName)
            }
            for (member in contract.fir.contractMembers()) {
                add(member.name)
                if (implementation) add(callbackName(member.name))
            }
        }
    }

    private fun callbackType(contract: FirRegularClassSymbol, member: FirNamedFunction, nullable: Boolean): ConeKotlinType =
        StandardClassIds.FunctionN(member.valueParameters.size + 1).createConeType(
            session,
            (listOf(contract.defaultType()) + member.valueParameters.map { it.symbol.resolvedReturnType } +
                member.symbol.resolvedReturnType).toTypedArray(),
            nullable = nullable,
        ).withAttributes(ConeAttributes.WithExtensionFunctionType)

    private fun hide(declaration: FirCallableDeclaration) {
        val levelId = ClassId.topLevel(FqName("kotlin.DeprecationLevel"))
        val levelClass = session.symbolProvider.getClassLikeSymbolByClassId(levelId) as FirRegularClassSymbol
        val entry = levelClass.fir.declarations.filterIsInstance<FirEnumEntry>().single { it.name.asString() == "HIDDEN" }
        declaration.replaceAnnotations(listOf(buildAnnotation {
            annotationTypeRef = ClassId.topLevel(FqName("kotlin.Deprecated")).createConeType(session).toFirResolvedTypeRef()
            argumentMapping = buildAnnotationArgumentMapping {
                mapping[Name.identifier("message")] = buildLiteralExpression(null, ConstantValueKind.String, "Binary compatibility", setType = true)
                mapping[Name.identifier("level")] = buildPropertyAccessExpression {
                    coneTypeOrNull = levelId.createConeType(session)
                    calleeReference = buildResolvedNamedReference {
                        name = entry.name
                        resolvedSymbol = entry.symbol
                    }
                }
            }
        }))
        declaration.replaceDeprecationsProvider(declaration.getDeprecationsProvider(session))
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val contract = contractFor(context.owner) ?: return emptyList()
        val methods = contract.fir.contractMembers()
        val implementation = context.owner.classId == implementationId(contract.classId)
        val lengths = if (implementation) listOf(methods.size) else contract.fir.prefixLengths().toList()
        return lengths.map { length ->
            createConstructor(context.owner, Key, isPrimary = length == methods.size) {
                visibility = contract.fir.status.visibility
                for (member in methods.take(length)) {
                    valueParameter(member.name, callbackType(contract, member, member.body != null),
                        hasDefaultValue = !implementation && member.body != null)
                }
            }.also { if (length != methods.size) hide(it) }.symbol
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val contract = contractFor(owner) ?: return emptyList()
        val implementation = owner.classId == implementationId(contract.classId)
        val member = contract.fir.contractMembers().firstOrNull {
            (if (implementation) callbackName(it.name) else it.name) == callableId.callableName
        } ?: return emptyList()
        return listOf(createMemberProperty(owner, Key, callableId.callableName,
            callbackType(contract, member, member.body != null)) {
            visibility = if (implementation) Visibilities.Private else contract.fir.status.visibility
        }.symbol)
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) {
            return contracts().filter {
                it.classId.packageFqName == callableId.packageName &&
                    (callableId.callableName == it.name || callableId.callableName == copyName)
            }.flatMap { contract ->
                val methods = contract.fir.contractMembers()
                val roles = buildList {
                    if (callableId.callableName == contract.name) add(false)
                    if (callableId.callableName == copyName) add(true)
                }
                roles.flatMap { copy ->
                    contract.fir.prefixLengths().map { length ->
                        createTopLevelFunction(Key, callableId, contract.defaultType(),
                            containingFileName = "${contract.name}${if (copy) "Copy" else "Constructor"}") {
                            visibility = contract.fir.status.visibility
                            if (copy) extensionReceiverType(contract.defaultType())
                            for (member in methods.take(length)) {
                                valueParameter(member.name, callbackType(contract, member, copy || member.body != null),
                                    hasDefaultValue = copy || member.body != null)
                            }
                        }.also { if (length != methods.size) hide(it) }.symbol
                    }
                }
            }
        }
        val owner = context.owner
        val sourceContract = contracts().firstOrNull { it.classId == owner.classId }
        val contract = sourceContract ?: contractFor(owner) ?: return emptyList()
        val implementation = owner.classId == implementationId(contract.classId)
        val name = callableId.callableName
        if (name == contractName && (sourceContract != null || implementation)) {
            return listOf(createMemberFunction(owner, Key, name, containerId(contract.classId).createConeType(session)) {
                if (sourceContract != null) modality = Modality.ABSTRACT else status { isOverride = true }
            }.symbol)
        }
        if (!implementation) return emptyList()
        val function = when (name.asString()) {
            "equals" -> createMemberFunction(owner, Key, name, session.builtinTypes.booleanType.coneType) {
                status { isOverride = true }
                valueParameter(Name.identifier("other"), session.builtinTypes.nullableAnyType.coneType)
            }
            "hashCode" -> createMemberFunction(owner, Key, name, session.builtinTypes.intType.coneType) {
                status { isOverride = true }
            }
            else -> {
                val member = contract.fir.contractMembers().firstOrNull { it.name == name } ?: return emptyList()
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
        override fun toString(): String = "Contract"
    }
}
