# Compose Contract

Kotlin K2 compiler plugin for generating value implementations of JVM component contracts.
The namespace and Gradle plugin ID are `com.alexcawl.contract`.
The plugin uses the Kotlin **2.4.20** compiler API and requires that compiler version.

## Usage

```kotlin
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Appearance {
    val width: Int
    val label: String?
    fun onClick(value: Int): Unit
    fun describe(prefix: String): String = "$prefix$label:$width"
}

val appearance: Appearance = Appearance(
    width = 24,
    label = "Button",
    onClick = { println(it) },
)
val wider: Appearance = appearance.copy(width = 48)
```

Generation happens in the compiler; it does not write Kotlin source files. The declarations
are available in Kotlin source and in compiled dependencies. All three declarations live
in the same package as the interface:

* `AppearanceImpl(width, label, onClick, describe)` implements the interface, stores its
  values and callbacks, and overrides `equals` and `hashCode`.
* `Appearance(width, label, onClick, describe = { prefix -> "$prefix$label:$width" })`
  returns `Appearance`, constructing `AppearanceImpl`. Default method bodies become
  default callback arguments. Properties remain required, including properties with getters.
* `Appearance.copy(width = this.width, label = this.label, onClick = this::onClick,
  describe = this::describe)` returns a new `AppearanceImpl` through the interface type.

Parameters are ordered by properties first, then methods, preserving declaration order
within each group. Methods with parameters become corresponding function types; a `vararg`
parameter becomes an array parameter in the callback. Both top-level functions have
`@JvmOverloads`. Their Java facade is `AppearanceContractKt`.
The generated implementation and functions preserve the interface's public/internal visibility.

`equals` compares only instances of the same generated implementation and includes every
value and callback using ordinary Kotlin equality. `hashCode` uses the same fields.
Arrays retain referential equality. Separate lambdas with identical code usually differ.

`copy` works with any implementation of the interface. Its default method references are
bound to the original receiver: `wider.describe("")` above still uses width `24`.
Override the callback explicitly to change this behavior. A copy containing method references
can compare unequal to its source, even when all property values are unchanged.

## Connect a local checkout

In the consuming project's `settings.gradle.kts`, register the checkout for plugin resolution
and dependency substitution:

```kotlin
pluginManagement {
    includeBuild("/path/to/compose-contract-plugin")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
includeBuild("/path/to/compose-contract-plugin")
```

In its `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("com.alexcawl.contract")
}
repositories { mavenCentral() }
```

The Gradle plugin applies only to Kotlin/JVM and Android JVM compilations, including JVM
and Android JVM targets in Kotlin Multiplatform projects. Declare contracts in JVM or
Android JVM source sets; shared and other platform source sets are unsupported. The plugin
adds the annotation dependency and orders this compiler plugin before Compose for supported
compilations.
No artifacts are published to Maven by this repository yet.

## Android Studio support

Editor support targets **Android Studio Quail 4 | 2026.1.4 Patch 1**
(build `261.26222.65.2614.16379836`). Keep Kotlin **2.4.20** in the consuming project
and in this plugin's build. Studio uses its own Kotlin compiler for editor analysis.
Other IDE versions are not covered by this compatibility target.

No additional IDE plugin is required. Enable loading external compiler plugins in Studio:

1. Open **Find Action** (`Cmd+Shift+A` on macOS) and select **Registry…**.
2. Find `kotlin.k2.only.bundled.compiler.plugins.enabled` and uncheck **Value** (`false`).
3. Build the compiler plugin with `./gradlew :compiler-plugin:jar` from this checkout.
4. Sync the consuming project with Gradle and restart Studio to reload the plugin JAR.

The editor should resolve the generated factory, implementation, and `copy` extension,
including inferred types and named-argument completion. Gradle remains responsible for
building and running the application with Kotlin 2.4.20. Rebuild the JAR, sync, and restart
Studio after changing the compiler plugin itself; editing a contract should update analysis
without a restart.

## Compose

Interfaces already expose `Any.equals` and `Any.hashCode`; this plugin supplies the value
semantics of their generated implementations. Equality alone does not establish Compose
stability. With strong skipping, unstable parameters are compared by identity, while stable
parameters are compared using `equals` ([Compose documentation](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping)).

When your interface and **all** its implementations satisfy the Compose stability contract,
annotate the interface with `androidx.compose.runtime.Stable`. The plugin does not add this
annotation automatically: a `val` can still hold mutable data, and custom implementations
must satisfy the same promise. Contract members themselves must not be `@Composable`.

## Current supported scope

Contracts are public or internal top-level interfaces with distinct public `val`/`fun` names.
Member types can be nullable or parameterized, such as `List<String>`.
Generic interfaces/methods, inheritance, overloads, `var`, suspend/extension/context members,
and sealed/expect/external contracts are rejected with compiler diagnostics.
`equals`, `hashCode`, and `copy` are reserved member names.

Default bodies can use properties, earlier methods, local variables, control flow, and other
ordinary Kotlin code. Because the bodies move to factory default arguments, they cannot use
standalone `this`, property references such as `this::width`, recursively call themselves,
or access later methods. Declare such methods abstract and supply callbacks explicitly,
or move their behavior outside the contract. Supplying a callback does not bypass validation
of an unsupported default body. A reference to an earlier contract method in a default
body delegates to the supplied callback's `invoke`; its reflection identity is not preserved.

### Unsupported declaration examples

These snippets intentionally fail plugin validation. Import
`com.alexcawl.contract.GenerateContract`; the Compose example also imports
`androidx.compose.runtime.Composable`.

The annotation only supports top-level public/internal interfaces:

```kotlin
@GenerateContract
class NotAnInterface // Classes and objects are not contracts.

class Components {
    @GenerateContract
    interface Nested // Nested contracts are not supported.
}

@GenerateContract
private interface Hidden // Use public or internal visibility.

@GenerateContract
sealed interface Closed // Sealed contracts are not supported.
```

Generic declarations and inheritance are not supported. Parameterized member types
such as `val items: List<String>` are supported.

```kotlin
@GenerateContract
interface GenericContract<T> { // Generic interface.
    val value: T
}

@GenerateContract
interface GenericMethod {
    fun <T> identity(value: T): T // Generic method.
}

interface HasWidth {
    val width: Int
}

@GenerateContract
interface InheritedSize : HasWidth // Even non-generic inheritance is unsupported.
```

Every property and method needs a distinct name, including methods with different signatures:

```kotlin
@GenerateContract
interface Overloaded {
    fun format(): String
    fun format(value: Int): String // Two callbacks would both be named format.
}

@GenerateContract
interface SameMemberName {
    val width: Int
    fun width(): Int // A property and a method cannot share a callback parameter name.
}

@GenerateContract
interface ReservedNames {
    override fun equals(other: Any?): Boolean // Reserved for generated equality.
    override fun hashCode(): Int // Reserved for generated hashing.
    fun copy(): ReservedNames // Reserved for the generated copy extension.
}
```

Mutable properties, suspend functions, member extensions, context members, non-public
members, and composable members are unsupported. Each member below illustrates a separate
restriction; making the other members valid does not remove that restriction.

```kotlin
@GenerateContract
interface UnsupportedMembers {
    var width: Int // Use val.

    suspend fun load(): String // Suspend callbacks are unsupported.

    fun String.decorate(): String // Member extension.

    context(prefix: String)
    fun describe(): String // Context parameter.

    private fun helper(): Int = 1 // Contract members must be public.

    @Composable
    fun render() // Composable method.

    @get:Composable
    val content: String // Composable getter.
}
```

### Unsupported default expressions

A default body cannot call a method declared later, even if that method has a default body:

```kotlin
@GenerateContract
interface ForwardCall {
    fun first(): Int = second() // Unsupported: second is declared later.
    fun second(): Int = 1
}
```

Reordering the methods makes this example supported:

```kotlin
@GenerateContract
interface OrderedCall {
    fun second(): Int = 1
    fun first(): Int = second() // Supported: second is an earlier callback parameter.
}
```

Direct and mutual recursion are unsupported:

```kotlin
@GenerateContract
interface Recursive {
    fun count(value: Int): Int =
        if (value == 0) 0 else count(value - 1) // Self-reference.
}

@GenerateContract
interface MutualRecursion {
    fun first(): Int = second() // Forward reference.
    fun second(): Int = first()
}
```

Reading a property through `this` is supported. Using the contract instance itself or
creating a reference to its property is unsupported. The same restrictions apply to
method parameter default expressions:

```kotlin
@GenerateContract
interface ReceiverExamples {
    val width: Int

    fun doubled(): Int = this.width * 2 // Supported: captures width.
    fun identity(): Any = this // Unsupported: requires the contract instance.
    fun widthReference(): () -> Int = this::width // Unsupported property reference.
    fun argument(value: Any = this): Any = value // Unsupported parameter default.
}
```

### Behavior that is not generated automatically

Property getter bodies do not become factory defaults:

```kotlin
@GenerateContract
interface DefaultWidth {
    val width: Int get() = 24
}

val missing = DefaultWidth() // Error: width is required.
val supplied = DefaultWidth(width = 48) // Supported; width is 48.
```

`copy` does not rebind methods to the new instance or guarantee equality with its source:

```kotlin
// Uses Appearance from the Usage example.
val original = Appearance(width = 24, label = "Button", onClick = {})
val changed = original.copy(width = 48)

check(changed.width == 48)
check(changed.describe("") == "Button:24") // describe is bound to original.
// original.copy() can compare unequal to original because callbacks are method references.
```

The plugin does not infer equivalent lambda bodies, add Compose stability annotations,
or preserve reflection identity when rewriting references to earlier methods in default bodies.
`@JvmOverloads` generates Java overloads for the factory and copy extension.

## Demos

The repository includes two independent applications:

* `demo-jvm` shows the generated factory, a default method, and `copy` in a console application.
  Its output also shows that copied callbacks remain bound to the original contract.
* `demo-android` runs a Material 3 counter built with Compose. The component accepts a
  `@Stable` contract with a title, a count, and an increment callback. The screen owns the
  count with `rememberSaveable`; a Preview is included.

Both demos load `compiler-plugin` through `kotlinCompilerPluginClasspath` and depend directly
on `plugin-annotations`, so no local Maven publication is needed. The Android demo orders
the contract compiler plugin before Compose. It uses AGP 9.4.1 with built-in Kotlin support;
the root Kotlin plugin declaration selects Kotlin 2.4.20 for all modules.

Use JDK 21. Install Android SDK Platform 36 and Build Tools 36.0.0, then set `ANDROID_HOME`
or create an ignored `local.properties` with `sdk.dir=/path/to/android/sdk`.

```shell
./gradlew :demo-jvm:run :demo-jvm:test
./gradlew :demo-android:assembleDebug :demo-android:testDebugUnitTest :demo-android:lintDebug
```

The debug APK is written to `demo-android/build/outputs/apk/debug/demo-android-debug.apk`.
Run `demo-android` from Android Studio on an emulator or device with API 23 or higher.

## Development

Use JDK 21, Android SDK Platform 36, and the Gradle 9.6.1 wrapper:

```shell
./gradlew :compiler-plugin:test
./gradlew :gradle-plugin:build
./gradlew build --continue
```

Compiler fixtures and FIR/IR expectations live in `compiler-plugin/testData`.
JUnit suites are generated into `compiler-plugin/build/test-gen`; do not commit them.
JVM box tests cover generation, default bodies, copy semantics, equality/hashCode,
JVM overloads, and consumption from another module. Diagnostics tests cover rejected contracts.

After an intentional annotation API change, run `./gradlew :plugin-annotations:updateKotlinAbi`
and review `plugin-annotations/api`. The build and tests require no JS or Native tooling.
