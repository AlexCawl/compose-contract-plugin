package com.alexcawl.contract.ir

import com.alexcawl.contract.fir.ContractDeclarationGenerator
import com.alexcawl.contract.fir.callbackName
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames

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

    private fun IrClass.members(): List<IrDeclarationWithName> =
        declarations.filterIsInstance<IrProperty>().filterNot { it.isFakeOverride } +
            declarations.filterIsInstance<IrSimpleFunction>().filterNot { it.isFakeOverride }

    private fun fields(implementation: IrClass, contract: IrClass): List<IrField> = contract.members().map { member ->
        implementation.properties.single {
            it.name == if (member is IrSimpleFunction) callbackName(member.name) else member.name
        }.backingField!!
    }

    fun generate(module: IrModuleFragment) {
        val declarations = module.files.flatMap { it.declarations }
        val implementations = declarations.filterIsInstance<IrClass>().filter { it.isGenerated() }
            .associateBy { it.superTypes.single().classOrNull!!.owner }
        for ((contract, implementation) in implementations) generateImplementation(contract, implementation)
        for (function in declarations.filterIsInstance<IrSimpleFunction>().filter { it.isGenerated() }) {
            val contract = function.returnType.classOrNull!!.owner
            generateFactory(function, contract, implementations.getValue(contract))
        }
    }

    private fun generateImplementation(contract: IrClass, implementation: IrClass) {
        val fields = fields(implementation, contract)
        val constructor = implementation.primaryConstructor!!
        val constructorBuilder = DeclarationIrBuilder(context, constructor.symbol)
        constructor.body = constructorBuilder.irBlockBody {
            +irDelegatingConstructorCall(builtIns.anyClass.owner.constructors.single())
            fields.zip(constructor.parameters).forEach { (field, parameter) ->
                +irSetField(irGet(implementation.thisReceiver!!), field, irGet(parameter))
            }
            +IrInstanceInitializerCallImpl(startOffset, endOffset, implementation.symbol, builtIns.unitType)
        }
        for (property in implementation.properties.filter { it.isGenerated() }) {
            val getter = property.getter!!
            getter.body = DeclarationIrBuilder(context, getter.symbol).irBlockBody {
                +irReturn(irGetField(irGet(getter.dispatchReceiverParameter!!), property.backingField!!))
            }
        }
        for (function in implementation.functions.filter { it.isGenerated() && it.correspondingPropertySymbol == null }) {
            val builder = DeclarationIrBuilder(context, function.symbol)
            function.body = builder.irBlockBody {
                val receiver = function.dispatchReceiverParameter!!
                when (function.name.asString()) {
                    "equals" -> {
                        val other = function.parameters.last()
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
                            result = irCallOp(
                                builtIns.intPlusSymbol, builtIns.intType,
                                irCallOp(builtIns.intTimesSymbol, builtIns.intType, result, irInt(31)), nullableHash,
                            )
                        }
                        +irReturn(result)
                    }
                    else -> {
                        val index = contract.members().indexOfFirst { it.name == function.name }
                        val field = fields[index]
                        val parameters = function.parameters.filter { it.kind == IrParameterKind.Regular }
                        +irReturn(invokeLambda(
                            builder, irGetField(irGet(receiver), field), field.type,
                            parameters.map { irGet(it) }, function.returnType,
                        ))
                    }
                }
            }
        }
    }

    private fun invokeLambda(
        builder: DeclarationIrBuilder,
        lambda: IrExpression,
        lambdaType: IrType,
        arguments: List<IrExpression>,
        returnType: IrType,
    ): IrExpression = builder.irCall(
        lambdaType.classOrNull!!.owner.functions.single { it.name.asString() == "invoke" }.symbol,
        returnType,
    ).apply {
        this.arguments[0] = lambda
        arguments.forEachIndexed { index, argument -> this.arguments[index + 1] = argument }
    }

    private fun generateFactory(function: IrSimpleFunction, contract: IrClass, implementation: IrClass) {
        val builder = DeclarationIrBuilder(context, function.symbol)
        val parameters = function.parameters.filter { it.kind == IrParameterKind.Regular }
        val receiver = function.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
        for ((member, parameter) in contract.members().zip(parameters)) {
            if (receiver != null) {
                val expression = when (member) {
                    is IrProperty -> builder.irCall(member.getter!!).apply { arguments[0] = builder.irGet(receiver) }
                    is IrSimpleFunction -> IrFunctionReferenceImpl(
                        function.startOffset, function.endOffset, parameter.type, member.symbol, typeArgumentsCount = 0,
                    ).apply { arguments[0] = builder.irGet(receiver) }
                    else -> error("Unexpected contract member")
                }
                parameter.defaultValue = context.irFactory.createExpressionBody(expression)
            } else if (member is IrSimpleFunction && member.body != null) {
                parameter.defaultValue = context.irFactory.createExpressionBody(defaultLambda(function, member, parameter, contract, parameters))
            }
        }
        function.body = builder.irBlockBody {
            +irReturn(irCallConstructor(implementation.primaryConstructor!!.symbol, emptyList()).apply {
                parameters.forEachIndexed { index, parameter -> arguments[index] = irGet(parameter) }
            })
        }
    }

    private fun defaultLambda(
        factory: IrSimpleFunction,
        method: IrSimpleFunction,
        parameter: IrValueParameter,
        contract: IrClass,
        parameters: List<IrValueParameter>,
    ): IrExpression {
        val lambda = method.deepCopyWithSymbols(factory).apply {
            name = SpecialNames.ANONYMOUS
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            modality = Modality.FINAL
            metadata = null
            overriddenSymbols = emptyList()
            annotations = emptyList()
        }
        val receiver = lambda.dispatchReceiverParameter!!
        lambda.parameters = lambda.parameters.filter { it.kind == IrParameterKind.Regular }.onEach {
            it.defaultValue = null
            it.varargElementType = null
        }
        val builder = DeclarationIrBuilder(context, lambda.symbol)
        val members = contract.members()
        lambda.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            private fun IrExpression?.isThis(): Boolean = this is IrGetValue &&
                (symbol == receiver.symbol || symbol.owner is IrValueParameter &&
                    (symbol.owner as IrValueParameter).kind == IrParameterKind.DispatchReceiver &&
                    (symbol.owner.parent as? IrSimpleFunction)?.parent == contract)

            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.dispatchReceiver.isThis()) return super.visitCall(expression)
                val memberIndex = members.indexOfFirst {
                    when (it) {
                        is IrProperty -> it.getter?.symbol == expression.symbol
                        is IrSimpleFunction -> it.symbol == expression.symbol
                        else -> false
                    }
                }
                check(memberIndex >= 0) { "Unsupported receiver use in default method ${method.name}" }
                val captured = parameters[memberIndex]
                if (members[memberIndex] is IrProperty) return builder.irGet(captured)
                val transformer = this
                return builder.irBlock(resultType = expression.type) {
                    val arguments = mutableMapOf<IrValueSymbol, IrVariable>()
                    for ((index, targetParameter) in expression.symbol.owner.parameters.withIndex()) {
                        if (targetParameter.kind != IrParameterKind.Regular) continue
                        val argument = expression.arguments[index]?.transform(transformer, null)
                            ?: targetParameter.defaultValue!!.expression.deepCopyWithSymbols(lambda).transform(
                                object : IrElementTransformerVoid() {
                                    override fun visitGetValue(expression: IrGetValue): IrExpression =
                                        arguments[expression.symbol]?.let { builder.irGet(it) } ?: expression
                                }, null,
                            ).transform(transformer, null)
                        arguments[targetParameter.symbol] = irTemporary(argument)
                    }
                    +invokeLambda(
                        builder, builder.irGet(captured), captured.type,
                        arguments.values.map { builder.irGet(it) }, expression.type,
                    )
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                if (!expression.dispatchReceiver.isThis()) return super.visitFunctionReference(expression)
                val memberIndex = members.indexOfFirst { it is IrSimpleFunction && it.symbol == expression.symbol }
                check(memberIndex >= 0) { "Unsupported callable reference in default method ${method.name}" }
                val captured = parameters[memberIndex]
                val invoke = captured.type.classOrNull!!.owner.functions.single { it.name.asString() == "invoke" }
                // shortcut: references reflect callback.invoke; generate adapters if method reflection identity is required.
                return IrFunctionReferenceImpl(
                    -1, -1, expression.type, invoke.symbol, typeArgumentsCount = 0,
                ).apply { arguments[0] = builder.irGet(captured) }
            }
        })
        // The copied body belongs to a generated file without source line information.
        lambda.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.startOffset = -1
                element.endOffset = -1
                element.acceptChildrenVoid(this)
            }
        })
        return IrFunctionExpressionImpl(factory.startOffset, factory.endOffset, parameter.type, lambda, IrStatementOrigin.LAMBDA)
    }
}
