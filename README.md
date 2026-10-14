# Contract Plugin

Kotlin K2 compiler plugin that generates implementations, factories, and `copy` extensions
for callback-based contracts. Requires Kotlin **2.4.20**. Supports Kotlin/JVM and Android JVM,
including those targets in Multiplatform projects. Compiled contract libraries need no plugin
in their consumers. The Gradle plugin adds annotations and runs before the Compose compiler.

## Usage

```kotlin
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Appearance {
    fun width(): Int
    fun label(): String = "Width: ${width()}"
}

val appearance: Appearance = Appearance(width = { 24 })
val wider = appearance.copy(width = { 48 })
check(wider.label() == "Width: 48")
check(appearance.copy() == appearance)
```

Abstract methods require callbacks; methods with bodies use their defaults unless overridden.
Callbacks receive the current instance as `this`, including after copying. A `null` copy argument
preserves its callback; it cannot reset an override to the interface default. Equality and hashing
compare stored callbacks, not their results.

See [demo contracts and tests](demo/src) and [API details](plugin-annotations/src/main/kotlin/com/alexcawl/contract/GenerateContract.kt).

## GitHub Packages

Releases publish the Gradle plugin, compiler plugin, and annotations to
[GitHub Packages](https://github.com/AlexCawl/contract-plugin/packages).
Use a personal access token **classic** with `read:packages`, even for public packages.
Keep credentials outside the project in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

Configure both repositories in the consuming project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.github.com/alexcawl/contract-plugin") {
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
        maven("https://maven.pkg.github.com/alexcawl/contract-plugin") {
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

Apply the plugin in `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("com.alexcawl.contract") version "2.4.20-1"
}
```

## Local checkout

Instead of GitHub Packages, add these entries to `settings.gradle.kts` alongside the usual
Maven Central and Gradle Plugin Portal repositories. Omit the contract plugin's version above.

```kotlin
pluginManagement { includeBuild("/path/to/contract-plugin") }
includeBuild("/path/to/contract-plugin")
```

## Supported contracts

Contracts must be public or internal top-level interfaces with distinct public method names.
Generic interfaces or methods, inheritance, properties, overloads, suspend, extension, context,
private, and `@Composable` methods are unsupported, as are sealed/expect/external interfaces.
Names `equals`, `hashCode`, `copy`, `contract`, and the nested classifier `Contract` are reserved.
Nullable, parameterized, and inline value types are supported; JVM parameter-slot limits apply.
Declare contracts in JVM or Android JVM source sets. Apply Compose `@Stable` yourself only when
the interface and all implementations satisfy its contract; callback equality does not ensure it.

## Evolving contracts

Mark new default methods with `@IntroducedAtContract(version)` using increasing positive versions.
Preserve published signatures, method order within each version, and JVM default mode.
The plugin retains hidden overloads for old binaries; with JVM default mode `DISABLE`, old manual
implementations must be recompiled to use new default methods. See the API details linked above.

## Android Studio

Editor support targets **Quail 4 | 2026.1.4 Patch 1**. In **Find Action → Registry…**, set
`kotlin.k2.only.bundled.compiler.plugins.enabled` to `false`. Run `./gradlew :compiler-plugin:jar`,
sync the consuming project, and restart Studio. Repeat after changing the compiler plugin.

## Development and releases

Use JDK 21 and the Gradle wrapper; no Android SDK is required. Run `./gradlew build --continue`
for all checks, or `./gradlew :compiler-plugin:test` for compiler tests.
Do not commit generated suites from `compiler-plugin/build/test-gen`.

Versions in `gradle.properties` follow `<Kotlin version>-<positive revision>`; increment the
revision for fixes and reset it to `1` when updating Kotlin. After CI passes, publish a GitHub
Release tagged `v<version>` (currently `v2.4.20-1`). The workflow checks the tag, builds, and
publishes all three plugin modules plus the Gradle plugin marker using `GITHUB_TOKEN`.
Publish fixes under new versions; do not overwrite or delete published packages.
