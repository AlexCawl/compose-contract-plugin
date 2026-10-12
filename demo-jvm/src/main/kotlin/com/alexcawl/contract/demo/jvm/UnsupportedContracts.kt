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
// interface GenericAppearance<T> { fun token(): T }
// @GenerateContract
// interface GenericMethod { fun <T> token(value: T): T }

// Inheritance is unsupported even when the parent is not generic or annotated.
// interface BaseAppearance { fun width(): Int }
// @GenerateContract
// interface InheritedAppearance : BaseAppearance

// Each method needs its own callback name, regardless of JVM signatures.
// @GenerateContract
// interface OverloadedAppearance {
//     fun color(): Long
//     fun color(selected: Boolean): Long
// }
// @GenerateContract
// interface CollidingAppearance {
//     fun color(): Int
//     fun Color(): Int // Both container getters would have the JVM name getColor.
// }

// equals, hashCode, copy, and contract are reserved for generated declarations.
// @GenerateContract
// interface ReservedAppearance {
//     override fun equals(other: Any?): Boolean
//     override fun hashCode(): Int
//     fun copy(): ReservedAppearance
//     fun contract(): Any
// }

// Each member below is independently unsupported: properties, suspend, extension, context, private, composable.
// @GenerateContract
// interface UnsupportedMembers {
//     val height: Int
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

// Required callbacks must be supplied to both the factory and the callback container.
// val missingPrefix = DefaultAppearance(token = { "#$it" })
// val missingToken = DefaultAppearanceContract(prefix = { "day:" })

// SlotLimitAppearance keeps its forbidden 128th Long parameter commented beside the working 127-parameter method.
// Default-argument masks, receivers, and markers also consume slots in generated JVM helpers.
// Consequently, the JVM's 255-slot limit is not a universal 255-field contract limit.
