# Compose Contract

Kotlin K2 compiler plugin for generating value implementations of JVM component contracts.
Contracts describe a component's presentation: dimensions, colors, and values derived from
display state. The screen owns state and actions; contract methods compute presentation values.
The namespace and Gradle plugin ID are `com.alexcawl.contract`.
The plugin uses the Kotlin **2.4.20** compiler API and requires that compiler version.

## Usage

```kotlin
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Appearance {
    fun width(): Int
    fun label(): String?
    fun foregroundColor(isSelected: Boolean): Long
    fun describe(prefix: String): String = "$prefix${label()}:${width()}"
}

val appearance: Appearance = Appearance(
    width = { 24 },
    label = { "Button" },
    foregroundColor = { selected -> if (selected) 0xFF6750A4 else 0xFFF3EDF7 },
)
val wider: Appearance = appearance.copy(width = { 48 })
check(wider.describe("") == "Button:48")
check(appearance.copy() == appearance)
```

Contract members are methods. Each factory argument is a typed callback with the contract
as its receiver, for example `Appearance.() -> Int` or `Appearance.(Boolean) -> Long`.
Abstract methods require arguments at compile time. Methods with bodies accept nullable
callbacks with default `null`; an absent callback invokes the original interface default.
The callback itself is tested for absence, so a callback returning `null` remains an override.

Callbacks execute with the current generated instance as `this`. Copying preserves the
callbacks and changes their receiver to the new instance. Interface default bodies remain
in the interface and use normal virtual dispatch, including forward references, recursion,
`this`, callable references, varargs, and method parameter defaults. A callback that explicitly
captures another object still retains that capture.

Generation happens in the compiler; physical Kotlin files are not written. The generated
API includes a nested callback container and package-level factory and copy functions:

* `Appearance.Contract` is a final class nested in the interface, with typed immutable callback
  properties. Its constructor orders callbacks by introduction version, then source order within
  each version. Required callbacks are non-null; default method callbacks are nullable with
  default `null`. It uses ordinary reference equality.
* A private `AppearanceImpl` stores callbacks directly. Only this class gets generated
  `equals` and `hashCode`, comparing callbacks with ordinary Kotlin equality. Distinct lambdas
  usually differ even when their bodies match. Results returned by callbacks are not compared.
* `Appearance(...)` returns `Appearance` and creates the private implementation. Its synthetic
  file and JVM facade are `AppearanceConstructor.kt` and `AppearanceConstructorKt`.
* `Appearance.copy(...)` returns `Appearance`. All callback arguments are nullable with default
  `null`, meaning preserve the previous callback. Its file and facade are `AppearanceCopy.kt`
  and `AppearanceCopyKt`. It calls `contract()` once, merges overrides, and invokes the factory.
* The interface gains `fun contract(): Appearance.Contract`. The generated implementation
  constructs a fresh container on each call; it does not store or cache one.

Container constructors, factories, copy functions, and private implementation constructors order
parameters by introduction version, preserving source order within each version. Container
and function visibility follows the public/internal interface. There is no member `copy`.
An unchanged copy has equal callbacks and the same hash, with distinct object identity.
Resetting a callback to its interface default is not supported by `copy`.

Manual implementations provide `contract()` themselves, defining the callbacks to transfer:

```kotlin
class ManualAppearance : Appearance {
    override fun width() = 24
    override fun label() = "Manual"
    override fun foregroundColor(isSelected: Boolean) = 0xFF6750A4
    override fun contract() = Appearance.Contract(
        width = { 24 },
        label = { "Manual" },
        foregroundColor = { 0xFF6750A4 },
    )
}

val copied: Appearance = ManualAppearance().copy(width = { 48 })
```

Copying a manual implementation creates the generated implementation. The plugin neither
casts the source to `AppearanceImpl` nor infers callbacks from a manual method's body.

## Evolving published contracts

Moving `AppearanceContract` to `Appearance.Contract` changes its JVM name to
`Appearance$Contract` and the return type of `contract()`. The old class is no longer
generated. Update explicit container references and rebuild contract libraries and consumers;
there is no compatibility alias or bridge for the old container API.

Mark newly added default methods with `@IntroducedAtContract(version: Int)` from
`com.alexcawl.contract`. Methods without the annotation belong to the initial version, `0`.
Explicit versions must be positive; gaps are allowed. Methods with the same version are
introduced together. Every annotated method must have a body. The annotation is only valid
on members of a `@GenerateContract` interface.

The plugin generates a container constructor, factory, and copy extension for the initial
version and each distinct annotated version. For initial methods `a, b`, methods `c, d`
annotated with version `1`, and method `e` annotated with version `3`, the signatures are
generated newest first: `abcde`, `abcd`, and `ab` (versions `3`, `1`, and `0`). The current
container constructor is primary; older constructors are secondary. Factories and copy
extensions follow the same descending version order. There is no intermediate `abc` signature.
This parameter order also applies to interleaved source methods such as `e, a, c, b, d`.

Methods may appear in any source order. A signature for version `v` contains methods with
version at most `v`, ordered by ascending version and then source order within each version.
Without annotations, only the full signature is generated. If all methods are versioned,
the initial signature is empty.
Ordinary Kotlin lowering may additionally generate a no-argument container constructor
when all current callbacks have defaults.

The full signature is the current source API. Older signatures carry
`@Deprecated(level = HIDDEN)` and retain their default arguments. Standard Kotlin lowering
produces their old `$default` methods and constructor mask overloads. Hidden overloads delegate
directly to the current signature, matching callbacks by name and supplying `null` for missing
callbacks. Old copies retain new callbacks already present on their source object.
`kotlin.IntroducedAt` and `@JvmOverloads` are not used.

After publication, preserve each version's method set and relative method order, along with
names, types, parameter names, default argument availability, and JVM owners. New methods may
be inserted anywhere in the interface; give them default bodies and a version greater than
every published version. Their callbacks are appended to generated signatures, preserving
ordinary positional calls with arguments inside parentheses and named Kotlin calls.
Adding a required method cannot preserve existing creation calls. Switching from the former
all-prefix generator can break existing binaries: overloads outside the declared version
boundaries are no longer generated. Compatibility applies between releases using the new scheme.
Switching from source-ordered parameters to version-ordered parameters also changes previously
generated mixed-version signatures. Rebuild those contract libraries and their clients.

The plugin does not set, override, or validate `JvmDefaultMode`. Unless configured by the
producer, Kotlin uses its default (`ENABLE` in Kotlin 2.4.20). Consumers of the compiled
library need no compiler plugin. The selected mode affects compatibility:

* `ENABLE` generates JVM default methods and `DefaultImpls` compatibility helpers. Old manual
  Kotlin and Java implementations can inherit newly added default methods.
* `NO_COMPATIBILITY` generates JVM default methods without `DefaultImpls`. New default methods
  can still be inherited, but switching from `ENABLE` can break binaries using the old helpers.
* `DISABLE` moves default bodies into static `DefaultImpls` methods; the interface methods
  are abstract on the JVM. Generated implementations and copies work, but old manual
  implementations do not acquire newly added methods. Calling such a method on an old
  implementation can throw `AbstractMethodError`; those implementations must be recompiled.
  Java implementations must provide these methods explicitly.

Keep the JVM mode unchanged after publication. Hidden factory and copy overloads preserve
their signatures but cannot supply missing methods on old implementation classes. See
[Kotlin's JVM default modes](https://kotlinlang.org/docs/java-to-kotlin-interop.html#compatibility-modes-for-default-methods).

The plugin does not register ABI validation tasks or manage compatibility baselines.
Configure API validation in the consuming project and ensure the validator reads compiled
outputs, including generated classes. API snapshots do not replace running old clients
against new binaries, including hidden overloads and default helpers.

This change requires one rebuild of existing contracts and consumers. Subsequent guarantees
cover old JVM binaries, ordinary positional Kotlin calls, and named Kotlin source calls under
the versioning rules above, subject to the JVM mode limitations above. New Java source uses
the current full signature and may need
additional explicit `null` arguments.
Callable reference arity and trailing lambda binding are not guaranteed across contract
evolution. Hidden JVM bridges remain callable by already compiled clients.

## Connect GitHub Packages

Published releases provide the Gradle plugin, compiler plugin, and annotations in
[GitHub Packages](https://github.com/AlexCawl/compose-contract-plugin/packages).
The Gradle plugin adds the compiler plugin and annotation dependency automatically.

GitHub requires authentication even for public Maven packages. Create a personal access
token **classic** with `read:packages`, and store your credentials in
`~/.gradle/gradle.properties`, outside the consuming project's repository:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

In the consuming project's `settings.gradle.kts`, configure both plugin and dependency
repositories:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.github.com/alexcawl/compose-contract-plugin") {
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.github.com/alexcawl/compose-contract-plugin") {
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

In its `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("com.alexcawl.contract") version "2.4.20-1"
}
```

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

## Publishing releases

`group` and `version` are defined in `gradle.properties`. Versions have the form
`<Kotlin version>-<positive revision>`, initially `2.4.20-1`. Increase the revision for
plugin fixes. When updating Kotlin in `gradle/libs.versions.toml`, update the version's
Kotlin prefix and reset the revision to `1`. Gradle rejects versions with a different
Kotlin prefix or an invalid revision.

After the Linux CI build succeeds for the release commit, publish a GitHub Release with
the matching tag, initially `v2.4.20-1`. The publication workflow runs for published
releases, including prereleases. It checks the tag, runs `./gradlew build --continue` on
Linux with JDK 21, then publishes `compiler-plugin`, `gradle-plugin`, `plugin-annotations`,
and the Gradle plugin marker. Demo modules and compiler test fixtures are not published.

The workflow uses GitHub Actions' built-in `GITHUB_TOKEN` with `packages: write`; no
additional publishing secret is required. Local builds do not require package credentials.
See [GitHub's Gradle registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
for authentication details.

Do not overwrite or delete published versions. Publish fixes under a new revision.
A failed or partially completed upload fails the workflow; it does not delete packages
or silently skip existing versions.

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

The editor should resolve the generated factory, callback container, and `copy` extension,
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
annotation automatically: callbacks can capture mutable data, and custom implementations
must satisfy the same promise. Contract members themselves must not be `@Composable`.

## Current supported scope

Contracts are public or internal top-level interfaces with distinct public `fun` names.
Member types can be nullable or parameterized, such as `List<String>`. Generic interfaces
and methods, interface inheritance, properties (`val` and `var`), overloads, suspend,
extension/context/Composable methods, private members, and sealed/expect/external contracts
are rejected with compiler diagnostics. `equals`, `hashCode`, `copy`, and `contract` are reserved.
Generated class names and callback getter JVM signatures must not collide.
The nested classifier name `Contract` is reserved. Unrelated package-level `<Name>Contract`
declarations are allowed.

A method's `vararg` becomes an array parameter in its callback. High arity callbacks are
supported within JVM parameter-slot limits. Generated default helpers also require slots for
masks, receivers, and markers; the maximum is not 255 contract methods.

Interface bodies keep ordinary Kotlin semantics. The plugin does not infer equivalent lambda
bodies, add Compose stability annotations, or make arbitrary captures immutable. See the
[commented invalid declarations](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/UnsupportedContracts.kt)
for unsupported examples.

## Demos

The repository includes two independent applications:

* `demo-android` shows a selectable week built with [CalendarDay](demo-android/src/main/kotlin/com/alexcawl/contract/demo/android/CalendarDay.kt).
  Size and appearance are separate contracts. `CalendarDaySizePreset` and `CalendarDayAppearancePreset`
  expose presets as `val` properties with `@Composable` getters. Defaults use 48 dp minimum dimensions,
  8 dp padding, and the current Material 3 colors and shapes. The weekend appearance uses
  `CalendarDayAppearancePreset.default.copy(foregroundColor = ...)`. The screen owns selection with
  `rememberSaveable` and `Modifier.selectable`, and pairs text colors with `contentColorFor`.
  Both system themes and light/dark Previews are included; the week scrolls on narrow screens.
* `demo-jvm` is a runnable catalogue of presentation contracts at the supported boundaries.
  It includes working examples, tests, and forbidden declarations left commented in source.

The JVM catalogue covers:

* [Compound values](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/ValueAppearance.kt):
  primitives, nullable and inline results, nested collections, arrays, returned functions,
  an empty contract, and an internal contract. Equality compares the stored callbacks.
* [Default methods](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/DefaultAppearance.kt):
  callback dependencies and callable references, parameter defaults, named argument evaluation,
  varargs, and returned functions. Copied methods execute on the copied instance.
* [65-field palette](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/WidePalette.kt):
  generated copy defaults cross the 32/33 and 64/65 parameter boundaries.
* [High arity and JVM slots](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/ArityAppearance.kt):
  a 23-argument method and a method with 127 `Long` parameters. The latter reaches the
  [JVM limit of 255 parameter slots](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3),
  including `this`; the forbidden 128th parameter stays commented. Generated default helpers
  also need slots for masks, receivers, and markers, so 255 slots does not mean 255 contract fields.
* [Java interoperability](demo-jvm/src/test/java/com/alexcawl/contract/demo/jvm/JvmAppearanceInteropTest.java):
  direct calls to current generated factory and copy signatures from Java.
* [Unsupported constructs](demo-jvm/src/main/kotlin/com/alexcawl/contract/demo/jvm/UnsupportedContracts.kt):
  commented examples explain declaration, member, and generated-name restrictions.
  Composable examples require no Compose dependency while commented.

Both demos load `compiler-plugin` through `kotlinCompilerPluginClasspath` and depend directly
on `plugin-annotations`, so no local Maven publication is needed. They use Kotlin's default
JVM mode. The Android demo orders the contract compiler plugin before Compose.
It uses AGP 9.4.1 with built-in Kotlin support;
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
versioned hidden overloads, and consumption from another module without the plugin.
Compatibility tests run old clients against new libraries and recompile their positional and
named calls without the plugin, including default-argument masks across the 32/33 and 64/65
boundaries. Diagnostics tests cover rejected contracts and versions.

After an intentional annotation API change, run `./gradlew :plugin-annotations:updateKotlinAbi`
and review `plugin-annotations/api`. The build and tests require no JS or Native tooling.
