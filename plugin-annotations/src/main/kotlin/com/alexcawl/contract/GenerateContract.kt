package com.alexcawl.contract

/**
 * Generates a callback-based value implementation, a nested callback container, a factory,
 * and a copy extension for an interface.
 *
 * Requires the Compose Contract compiler plugin on Kotlin/JVM or Android JVM, including
 * those targets in Kotlin Multiplatform projects. Declarations are generated in the compiler;
 * no physical Kotlin source files are written. Consumers of a compiled contract library
 * do not need the compiler plugin.
 *
 * ## Declaring and using a contract
 *
 * Declare presentation values as functions. Abstract functions require factory callbacks;
 * functions with bodies accept optional callbacks and use their original bodies by default.
 *
 * ```kotlin
 * @GenerateContract
 * interface Appearance {
 *     fun width(): Int
 *     @IntroducedAtContract(1)
 *     fun label(prefix: String): String = "$prefix${width()}"
 * }
 * ```
 *
 * Each callback has the interface as its receiver and takes the original function's arguments.
 * In this example, the callback types are `Appearance.() -> Int` and
 * `Appearance.(String) -> String`.
 *
 * ```kotlin
 * val appearance: Appearance = Appearance(width = { 24 })
 * val wider: Appearance = appearance.copy(width = { 48 })
 * check(wider.label("width:") == "width:48")
 * check(appearance.copy() == appearance)
 * check(appearance.copy() !== appearance)
 * ```
 *
 * Callbacks execute with the current generated instance as `this`. A preserved callback uses
 * the new instance after copying, so calls to other contract functions see copied overrides.
 * Explicitly captured objects retain their original references.
 *
 * ## Generated declarations
 *
 * The following is a readable Kotlin equivalent of the generated declarations for the example.
 * The original interface keeps its default body and gains `contract()` and `Appearance.Contract`.
 * Constructor parameters use the original method names. Immutable callback properties are
 * generated separately; the assignments below represent field initialization performed by
 * the generated constructor.
 *
 * The factory and private implementation belong to `AppearanceConstructor.kt`
 * (`AppearanceConstructorKt` on the JVM). The copy extensions belong to `AppearanceCopy.kt`
 * (`AppearanceCopyKt` on the JVM), in the interface's package. Standard Kotlin lowering also
 * generates default-argument helpers and JVM default-method machinery, which are not shown.
 *
 * ```kotlin
 * interface Appearance {
 *     fun width(): Int
 *     @IntroducedAtContract(1)
 *     fun label(prefix: String): String = "$prefix${width()}"
 *
 *     fun contract(): Appearance.Contract
 *
 *     class Contract(
 *         width: Appearance.() -> Int,
 *         label: (Appearance.(String) -> String)? = null,
 *     ) {
 *         val width: Appearance.() -> Int = width
 *         val label: (Appearance.(String) -> String)? = label
 *
 *         @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
 *         constructor(width: Appearance.() -> Int) : this(width, null)
 *     }
 * }
 *
 * // AppearanceConstructor.kt
 * private class AppearanceImpl(
 *     width: Appearance.() -> Int,
 *     label: (Appearance.(String) -> String)?,
 * ) : Appearance {
 *     private val `$contract$width`: Appearance.() -> Int = width
 *     private val `$contract$label`: (Appearance.(String) -> String)? = label
 *
 *     override fun width(): Int = `$contract$width`(this)
 *
 *     override fun label(prefix: String): String {
 *         val callback = `$contract$label`
 *         return if (callback == null) super<Appearance>.label(prefix) else callback(this, prefix)
 *     }
 *
 *     override fun contract(): Appearance.Contract =
 *         Appearance.Contract(`$contract$width`, `$contract$label`)
 *
 *     override fun equals(other: Any?): Boolean {
 *         if (this === other) return true
 *         if (other !is AppearanceImpl) return false
 *         return `$contract$width` == other.`$contract$width` &&
 *             `$contract$label` == other.`$contract$label`
 *     }
 *
 *     override fun hashCode(): Int =
 *         31 * `$contract$width`.hashCode() + (`$contract$label`?.hashCode() ?: 0)
 * }
 *
 * fun Appearance(
 *     width: Appearance.() -> Int,
 *     label: (Appearance.(String) -> String)? = null,
 * ): Appearance = AppearanceImpl(width, label)
 *
 * @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
 * fun Appearance(width: Appearance.() -> Int): Appearance = Appearance(width, null)
 *
 * // AppearanceCopy.kt
 * fun Appearance.copy(
 *     width: (Appearance.() -> Int)? = null,
 *     label: (Appearance.(String) -> String)? = null,
 * ): Appearance {
 *     val original = contract()
 *     return Appearance(width ?: original.width, label ?: original.label)
 * }
 *
 * @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
 * fun Appearance.copy(width: (Appearance.() -> Int)? = null): Appearance =
 *     copy(width = width, label = null)
 * ```
 *
 * `Appearance.Contract` is final and uses ordinary reference equality. Its callback properties
 * are immutable, ordered as the source functions, and typed with the original argument and
 * return types. The container constructor, factory, and copy extension follow the interface's
 * public or internal visibility. The private implementation stores callbacks directly;
 * `contract()` creates a fresh container on every call and does not cache it.
 * Generated constructor, factory, and copy parameters are ordered by introduction version,
 * preserving source order within each version. The private implementation constructor uses
 * the same order.
 *
 * Only the generated implementation receives value equality and hashing. Callbacks are compared
 * using their own `equals` and `hashCode`; callback results are never evaluated for comparison.
 * Distinct lambdas usually differ even when their bodies are identical. An unchanged copy is
 * a distinct instance with equal callbacks and the same hash code.
 *
 * ## Defaults and copying
 *
 * A callback for an abstract function is non-null and required by the factory and container
 * constructor. A callback for a function with a body is nullable and defaults to `null`, which
 * invokes that function's original interface body. A non-null callback returning `null` still
 * overrides a function with a nullable return type; only an absent callback triggers fallback.
 * Interface default bodies keep normal virtual dispatch, parameter defaults, and callable
 * references. A `vararg` parameter becomes an array parameter in its callback.
 *
 * All copy arguments are nullable and default to `null`, meaning preserve the corresponding
 * source callback. Copying cannot reset an override to its interface default. The copy extension
 * calls `contract()` exactly once, merges callbacks, and invokes the factory. Manual interface
 * implementations must implement `contract()` and supply the callbacks to transfer; copying
 * them creates the generated implementation without casting them to the private implementation.
 * There is no generated member `copy`.
 *
 * ## Evolving a published contract
 *
 * Mark new default methods with [IntroducedAtContract], using a positive integer version.
 * Unannotated methods belong to the initial version, `0`. Methods may appear in any source order;
 * gaps between versions are allowed, and methods with the same version are introduced together.
 * A signature for version `v` includes all methods introduced at or before `v`, ordered by
 * ascending version and then source order within each version.
 * Overloads are generated newest first: the current primary container constructor precedes
 * older secondary constructors. Factories and copy extensions follow the same descending order.
 * Without version annotations, only the full signature is generated. If every method is versioned,
 * the initial signature is empty; an empty interface is also supported.
 *
 * In this example, the initial one-argument container constructor, factory, and copy extension
 * are hidden from new Kotlin source by `DeprecationLevel.HIDDEN`. Older signatures retain default
 * arguments and delegate to the current full signature with `null` for missing callbacks. Kotlin
 * lowering retains the corresponding default-argument helpers for already compiled callers.
 * Old copies preserve newer callbacks already present on the source object.
 *
 * Preserve each published version's function set and relative function order, along with names,
 * types, parameter names, parameter default availability, and JVM owners. New functions may be
 * inserted anywhere but must have default bodies and a version greater than every published
 * version. Their callbacks are appended, preserving ordinary positional calls inside parentheses
 * and named Kotlin calls. Keep the producer's JVM default mode unchanged; this plugin does not
 * configure it. With `DISABLE`, old
 * manual implementations cannot inherit newly added default functions and must be recompiled.
 * New Java source uses current full signatures and may need explicit `null` arguments. Callable
 * reference arity and trailing-lambda binding are not compatibility guarantees.
 * Switching from the former all-prefix generator can break binaries: signatures outside the
 * declared version boundaries are no longer generated.
 * Switching from source-ordered to version-ordered parameters also requires rebuilding libraries
 * with mixed-version signatures and their clients.
 *
 * ## Supported declarations
 *
 * The annotated declaration must be a public or internal, non-generic, top-level interface
 * without interface inheritance. Its contract members must be distinct public, non-suspend
 * functions. Generic functions, extension or context members, properties, overloads, private
 * functions, `@Composable` functions, and sealed, expect, or external interfaces are unsupported.
 * Nullable, parameterized, and inline value types are supported in function signatures.
 *
 * Function names `equals`, `hashCode`, `copy`, and `contract`, and the nested classifier name
 * `Contract`, are reserved. Generated implementation and facade names and callback getter JVM
 * signatures must not collide with other declarations. JVM parameter-slot limits also apply
 * to callbacks and generated default-argument helpers.
 *
 * The plugin does not add Compose stability annotations or make callback captures immutable.
 * Apply `@Stable` yourself only when the interface and all its implementations satisfy that
 * contract; generated callback equality alone does not establish Compose stability.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class GenerateContract
