package com.alexcawl.contract.ir

import com.alexcawl.contract.fir.ContractDeclarationGenerator
import com.alexcawl.contract.fir.callbackName
import com.alexcawl.contract.fir.containerName
import com.alexcawl.contract.fir.generateContractId
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

class ContractIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        ContractBodyGenerator(pluginContext).generate(moduleFragment)
    }
}

private class ContractBodyGenerator(private val context: IrPluginContext) {
    private val builtIns = context.irBuiltIns
    private val anyHashCode = builtIns.anyClass.owner.functions.single { it.name.asString() == "hashCode" }

    private fun IrDeclaration.isGenerated(): Boolean =
        (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey == ContractDeclarationGenerator.Key

    private fun IrFunction.regularParameters(): List<IrValueParameter> = parameters.filter { it.kind == IrParameterKind.Regular }

    fun generate(module: IrModuleFragment) {
        val declarations = module.files.flatMap { it.declarations }
        val classes = declarations.filterIsInstance<IrClass>()
        val functions = declarations.filterIsInstance<IrSimpleFunction>().filter { it.isGenerated() }
        for (contract in classes.filter { it.hasAnnotation(generateContractId.asSingleFqName()) }) {
            val generated = classes.filter { it.isGenerated() && it.packageFqName == contract.packageFqName }
            val implementation = generated.single { it.name.asString() == "${contract.name}Impl" }
            val container = contract.declarations.filterIsInstance<IrClass>().single { it.isGenerated() && it.name == containerName }
            val sourceMethods = contract.declarations.filterIsInstance<IrSimpleFunction>().filterNot { it.isGenerated() || it.isFakeOverride }
            val factories = functions.filter { it.returnType.classOrNull == contract.symbol && it.parameters.none { p -> p.kind == IrParameterKind.ExtensionReceiver } }
            val copies = functions.filter { it.returnType.classOrNull == contract.symbol && it.parameters.any { p -> p.kind == IrParameterKind.ExtensionReceiver } }
            val factory = factories.single { it.regularParameters().size == sourceMethods.size }
            val copy = copies.single { it.regularParameters().size == sourceMethods.size }
            val methodsByName = sourceMethods.associateBy { it.name }
            val methods = factory.regularParameters().map { methodsByName.getValue(it.name) }
            val factoryFile = factory.parent as IrFile
            implementation.file.declarations.remove(implementation)
            implementation.parent = factoryFile
            factoryFile.declarations.add(implementation)
            generateStorage(container, methods, implementation = false)
            generateStorage(implementation, methods, implementation = true)
            generateImplementation(contract, implementation, container, methods)
            factories.forEach { generateFactory(it, factory, implementation) }
            copies.forEach { generateCopy(it, copy, factory, contract, container, methods) }
        }
    }

    private fun fields(owner: IrClass, methods: List<IrSimpleFunction>, implementation: Boolean): List<IrField> =
        methods.map { method -> owner.properties.single {
            it.name == if (implementation) callbackName(method.name) else method.name
        }.backingField!! }

    private fun generateStorage(owner: IrClass, methods: List<IrSimpleFunction>, implementation: Boolean) {
        val fields = fields(owner, methods, implementation)
        val primary = owner.primaryConstructor!!
        for (constructor in owner.constructors) {
            val parameters = constructor.regularParameters()
            parameters.filter { it.defaultValue != null }.forEach {
                it.defaultValue = context.irFactory.createExpressionBody(DeclarationIrBuilder(context, constructor.symbol).irNull(it.type))
            }
            constructor.body = DeclarationIrBuilder(context, constructor.symbol).irBlockBody {
                if (constructor == primary) {
                    +irDelegatingConstructorCall(builtIns.anyClass.owner.constructors.single())
                    fields.zip(parameters).forEach { (field, parameter) ->
                        +irSetField(irGet(owner.thisReceiver!!), field, irGet(parameter))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, owner.symbol, builtIns.unitType)
                } else {
                    +irDelegatingConstructorCall(primary).apply {
                        primary.regularParameters().forEachIndexed { index, parameter ->
                            arguments[index] = parameters.firstOrNull { it.name == parameter.name }?.let { irGet(it) }
                                ?: irNull(parameter.type)
                        }
                    }
                }
            }
        }
        for (property in owner.properties.filter { it.isGenerated() }) {
            val getter = property.getter!!
            getter.body = DeclarationIrBuilder(context, getter.symbol).irBlockBody {
                +irReturn(irGetField(irGet(getter.dispatchReceiverParameter!!), property.backingField!!))
            }
        }
    }

    private fun generateImplementation(contract: IrClass, implementation: IrClass, container: IrClass, methods: List<IrSimpleFunction>) {
        val fields = fields(implementation, methods, implementation = true)
        for (function in implementation.functions.filter { it.isGenerated() && it.correspondingPropertySymbol == null }) {
            val builder = DeclarationIrBuilder(context, function.symbol)
            function.body = builder.irBlockBody {
                val receiver = function.dispatchReceiverParameter!!
                when (function.name.asString()) {
                    "equals" -> {
                        val other = function.regularParameters().single()
                        +irIfThenReturnTrue(irEqeqeq(irGet(receiver), irGet(other)))
                        +irIfThenReturnFalse(irNotIs(irGet(other), implementation.defaultType))
                        val typedOther = irTemporary(irAs(irGet(other), implementation.defaultType))
                        for (field in fields) {
                            val same = irEquals(
                                irImplicitCast(irGetField(irGet(receiver), field), builtIns.anyNType),
                                irImplicitCast(irGetField(irGet(typedOther), field), builtIns.anyNType),
                            )
                            +irIfThenReturnFalse(irEquals(same, irFalse()))
                        }
                        +irReturn(irTrue())
                    }
                    "hashCode" -> {
                        var result: IrExpression = irInt(0)
                        for (field in fields) {
                            val hash = irCall(anyHashCode).apply { arguments[0] = irGetField(irGet(receiver), field) }
                            val nullableHash = if (field.type.isNullable()) {
                                irIfThenElse(builtIns.intType, irEqualsNull(irGetField(irGet(receiver), field)), irInt(0), hash)
                            } else hash
                            result = irCallOp(builtIns.intPlusSymbol, builtIns.intType,
                                irCallOp(builtIns.intTimesSymbol, builtIns.intType, result, irInt(31)), nullableHash)
                        }
                        +irReturn(result)
                    }
                    "contract" -> +irReturn(irCallConstructor(container.primaryConstructor!!.symbol, emptyList()).apply {
                        fields.forEachIndexed { index, field -> arguments[index] = irGetField(irGet(receiver), field) }
                    })
                    else -> {
                        val index = methods.indexOfFirst { it.name == function.name }
                        val method = methods[index]
                        val field = fields[index]
                        val callback = irGetField(irGet(receiver), field)
                        val invoke = invokeLambda(builder, irImplicitCast(callback, field.type.makeNotNull()), field.type.makeNotNull(),
                            listOf(irGet(receiver)) + function.regularParameters().map { irGet(it) }, function.returnType)
                        +irReturn(if (field.type.isNullable()) {
                            val fallback = irCall(method.symbol).apply {
                                superQualifierSymbol = contract.symbol
                                arguments[0] = irGet(receiver)
                                function.regularParameters().forEachIndexed { i, parameter -> arguments[i + 1] = irGet(parameter) }
                            }
                            irIfThenElse(function.returnType, irEqualsNull(irGetField(irGet(receiver), field)), fallback, invoke)
                        } else invoke)
                    }
                }
            }
        }
    }

    private fun invokeLambda(builder: DeclarationIrBuilder, lambda: IrExpression, type: IrType,
                             arguments: List<IrExpression>, returnType: IrType): IrExpression = builder.irCall(
        type.classOrNull!!.owner.functions.single { it.name.asString() == "invoke" }.symbol, returnType,
    ).apply {
        this.arguments[0] = lambda
        arguments.forEachIndexed { index, argument -> this.arguments[index + 1] = argument }
    }

    private fun defaults(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(context, function.symbol)
        function.regularParameters().filter { it.defaultValue != null }.forEach {
            it.defaultValue = context.irFactory.createExpressionBody(builder.irNull(it.type))
        }
    }

    private fun generateFactory(function: IrSimpleFunction, latest: IrSimpleFunction, implementation: IrClass) {
        defaults(function)
        val parameters = function.regularParameters()
        function.body = DeclarationIrBuilder(context, function.symbol).irBlockBody {
            +irReturn(if (function == latest) {
                irCallConstructor(implementation.primaryConstructor!!.symbol, emptyList()).apply {
                    parameters.forEachIndexed { index, parameter -> arguments[index] = irGet(parameter) }
                }
            } else {
                irCall(latest).apply {
                    latest.regularParameters().forEachIndexed { index, parameter ->
                        arguments[index] = parameters.firstOrNull { it.name == parameter.name }?.let { irGet(it) }
                            ?: irNull(parameter.type)
                    }
                }
            })
        }
    }

    private fun generateCopy(function: IrSimpleFunction, latest: IrSimpleFunction, factory: IrSimpleFunction,
                             contract: IrClass, container: IrClass, methods: List<IrSimpleFunction>) {
        defaults(function)
        val parameters = function.regularParameters()
        val receiver = function.parameters.single { it.kind == IrParameterKind.ExtensionReceiver }
        function.body = DeclarationIrBuilder(context, function.symbol).irBlockBody {
            if (function != latest) {
                +irReturn(irCall(latest).apply {
                    arguments[0] = irGet(receiver)
                    latest.regularParameters().forEachIndexed { index, parameter ->
                        arguments[index + 1] = parameters.firstOrNull { it.name == parameter.name }?.let { irGet(it) }
                            ?: irNull(parameter.type)
                    }
                })
            } else {
                val original = irTemporary(irCall(contract.functions.single { it.name.asString() == "contract" }).apply {
                    arguments[0] = irGet(receiver)
                }, "original")
                +irReturn(irCall(factory).apply {
                    parameters.forEachIndexed { index, parameter ->
                        val getter = container.properties.single { it.name == methods[index].name }.getter!!
                        val old = irCall(getter).apply { arguments[0] = irGet(original) }
                        arguments[index] = irIfThenElse(factory.regularParameters()[index].type,
                            irEqualsNull(irGet(parameter)), old, irImplicitCast(irGet(parameter), factory.regularParameters()[index].type))
                    }
                })
            }
        }
    }
}
