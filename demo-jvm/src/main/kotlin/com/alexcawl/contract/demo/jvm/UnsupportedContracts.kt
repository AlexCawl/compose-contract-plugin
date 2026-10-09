package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

// These examples stay commented so the catalogue compiles. Uncomment one to inspect its diagnostic.

// Only public/internal top-level interfaces can be annotated.
// @GenerateContract
// class ClassAppearance
// @GenerateContract
// object ObjectAppearance
// @GenerateContract
// private interface PrivateAppearance
// @GenerateContract
// sealed interface SealedAppearance
// @GenerateContract
// expect interface ExpectedAppearance
// @GenerateContract
// external interface ExternalAppearance
// class Container {
//     @GenerateContract
//     interface NestedAppearance
// }
// fun localAppearance() {
//     @GenerateContract
//     interface LocalAppearance
// }

// Generic declarations are unsupported; parameterized member types are supported.
// @GenerateContract
// interface GenericAppearance<T> { val token: T }
// @GenerateContract
// interface GenericMethod { fun <T> token(value: T): T }

// Inheritance is unsupported even when the parent is not generic or annotated.
// interface BaseAppearance { val width: Int }
// @GenerateContract
// interface InheritedAppearance : BaseAppearance

// Each property and method needs its own name, regardless of JVM signatures.
// @GenerateContract
// interface OverloadedAppearance {
//     fun color(): Long
//     fun color(selected: Boolean): Long
// }
// @GenerateContract
// interface CollidingAppearance {
//     val width: Int
//     fun width(): Int
// }

// equals, hashCode, and copy are reserved for generated declarations.
// @GenerateContract
// interface ReservedAppearance {
//     override fun equals(other: Any?): Boolean
//     override fun hashCode(): Int
//     fun copy(): ReservedAppearance
// }

// Each member below is independently unsupported: var, suspend, extension, context, private, composable.
// @GenerateContract
// interface UnsupportedMembers {
//     var width: Int
//     suspend fun measure(): Int
//     fun String.decorate(): String
//     val String.width: Int
//     context(prefix: String)
//     fun contextLabel(): String
//     private fun inset(): Int = 8
//     @androidx.compose.runtime.Composable
//     fun content()
//     @get:androidx.compose.runtime.Composable
//     val label: String
// }

// A default body cannot call a later method. Put dependencies first, as in DefaultAppearance.
// @GenerateContract
// interface ForwardAppearance {
//     fun first(): Int = second()
//     fun second(): Int = 1
// }

// Recursive default bodies cannot become independent factory callbacks.
// @GenerateContract
// interface RecursiveAppearance {
//     fun inset(depth: Int): Int = if (depth == 0) 0 else inset(depth - 1)
// }
// @GenerateContract
// interface MutuallyRecursiveAppearance {
//     fun first(): Int = second()
//     fun second(): Int = first()
// }

// Property reads through this are supported; standalone this and property references are not.
// @GenerateContract
// interface ReceiverAppearance {
//     val width: Int
//     fun identity(): Any = this
//     fun reference(): () -> Int = this::width
//     fun argument(value: Any = this): Any = value
//     fun referenceArgument(value: () -> Int = ::width): Int = value()
// }

// Supplying a callback does not bypass validation of an unsupported default body.
// val forward = ForwardAppearance(first = { 1 })

// A getter does not become a factory default: spacing remains required.
// val missingSpacing = DefaultAppearance(prefix = "day:", token = { "#$it" })

// SlotLimitAppearance keeps its forbidden 128th Long parameter commented beside the working 127-parameter method.
// Default-argument masks, receivers, and markers also consume slots in generated JVM helpers.
// Consequently, the JVM's 255-slot limit is not a universal 255-field contract limit.
