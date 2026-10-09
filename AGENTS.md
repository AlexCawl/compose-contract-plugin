# Repository Guidelines

## Project Structure & Module Organization

This repository uses the Kotlin compiler plugin template with three Kotlin/JVM Gradle modules:

- `compiler-plugin/src/`: compiler implementation, with FIR extensions in `fir/` and IR transformations in `ir/`. Service registrations live in `compiler-plugin/resources/META-INF/services/`.
- `gradle-plugin/src/`: Gradle integration that supplies the compiler plugin, annotation dependency, and plugin ordering before Compose.
- `plugin-annotations/src/main/kotlin/`: public JVM annotations; the JVM API baseline lives in `plugin-annotations/api/`.
- `compiler-plugin/test-fixtures/`: test runners, services, and test generation. Input programs and expected dumps live in `compiler-plugin/testData/`.

Dependency versions are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Use JDK 21, matching CI, and run the Gradle wrapper from the repository root:

- `./gradlew build --continue`: build modules and run verification; this is the CI command.
- `./gradlew :compiler-plugin:test`: run JVM compiler tests.
- `./gradlew :compiler-plugin:generateTests`: regenerate JUnit test classes; compilation also triggers this automatically.
- `./gradlew :gradle-plugin:build`: build and validate Gradle integration.

Only Kotlin/JVM and Android JVM compilations are supported, including JVM and Android JVM targets in consuming Multiplatform projects. CI runs on macOS. Initial builds may download dependencies; JS and Native tooling is not required.

## Coding Style & Naming Conventions

Follow official Kotlin style (`kotlin.code.style=official`), with four-space indentation, `PascalCase` classes, and `camelCase` functions and properties. Keep the existing `org.jetbrains.kotlin.compiler.plugin.template` package layout. Declare public annotation APIs explicitly, as required by `explicitApi()`.

No formatter or linter task is configured; use IDE Kotlin formatting. Keep compiler and Gradle plugin IDs consistent with generated `BuildConfig` values and service registrations.

## Testing Guidelines

Tests use the Kotlin compiler test framework with generated JUnit 5 suites. Add descriptive camelCase `.kt` fixtures under `testData/box/` for code generation or `testData/diagnostics/` for diagnostics. Box tests expose `fun box(): String` returning `"OK"` on success; diagnostics fixtures mark expected errors inline.

Review accompanying `.fir.txt` and `.ir.txt` expectations when behavior changes. Generated suites belong in `compiler-plugin/build/test-gen/`; do not edit or commit them. No numeric coverage threshold is configured; add regression fixtures for compiler behavior changes.

## Commit & Pull Request Guidelines

History contains only `Initial commit`, so no established commit convention exists. Use concise imperative subjects, such as `Fix IR generation for annotated functions`.

Keep PRs focused. Describe the problem, resulting behavior, affected modules, and verification commands/results. Link relevant issues and explain intentional changes to compiler dumps or public API baselines. Run the CI build before requesting review.
