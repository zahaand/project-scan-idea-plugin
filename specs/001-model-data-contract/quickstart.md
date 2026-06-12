# Quickstart: Using the `model` Component

**Feature**: 001-model-data-contract  
**Date**: 2026-06-12

---

## 1. Add the Gradle submodule

**`settings.gradle.kts`** — include the new module:

```kotlin
include(":model")
```

**`model/build.gradle.kts`** — pure Kotlin library, no IntelliJ Platform plugin:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.junit)
    // No intellijPlatform { } block — model must not depend on the platform SDK
}
```

**Root `build.gradle.kts`** — add `:model` as a dependency of the plugin module:

```kotlin
dependencies {
    implementation(project(":model"))
    // ... existing dependencies ...
}
```

---

## 2. Source layout

```text
model/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── kotlin/
    │       └── dev/zahaand/projectscan/model/
    │           ├── ProjectScanModel.kt
    │           ├── StackInfo.kt
    │           ├── CodeStyleInfo.kt
    │           ├── LinterInfo.kt
    │           ├── TestInfo.kt
    │           └── StructureInfo.kt
    └── test/
        └── kotlin/
            └── dev/zahaand/projectscan/model/
                ├── ProjectScanModelTest.kt
                ├── StackInfoTest.kt
                ├── CodeStyleInfoTest.kt
                ├── LinterInfoTest.kt
                ├── TestInfoTest.kt
                └── StructureInfoTest.kt
```

Files may be split further per type if preferred; all types live in the single package `dev.zahaand.projectscan.model`.

---

## 3. Constructing an empty model (producer — nothing detected)

```kotlin
val empty = ProjectScanModel(
    stack     = StackInfo(),
    codeStyle = CodeStyleInfo(),
    linters   = LinterInfo(),
    tests     = TestInfo(),
    structure = StructureInfo()
)
```

---

## 4. Constructing a populated model (producer — typical project)

```kotlin
val model = ProjectScanModel(
    stack = StackInfo(
        dependencies = listOf(
            Dependency("org.springframework.boot", "spring-boot-starter", "3.2.0"),
            Dependency("org.junit.jupiter", "junit-jupiter", "5.10.0")
        ),
        jdkVersion    = "21",
        languageLevel = "21",
        buildSystem   = BuildSystem.GRADLE
    ),
    codeStyle = CodeStyleInfo(
        sources = listOf(
            StyleSource(StyleSourceType.CHECKSTYLE, "config/checkstyle/checkstyle.xml"),
            StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig")
        )
    ),
    linters = LinterInfo(
        activeRules = listOf(
            ActiveRule("LineLength", "Checkstyle", RuleSeverity.ERROR, breaksBuild = true)
        )
    ),
    tests = TestInfo(
        frameworks        = listOf(TestFramework("JUnit 5", "5.10.0")),
        sourceRoots       = listOf("src/test/kotlin"),
        namingPattern     = "**/*Test.kt",
        coverageThreshold = 80.0
    ),
    structure = StructureInfo(
        modules = listOf(
            Module(
                name                 = "app",
                declaredDependencies = listOf(Dependency("org.springframework.boot", "spring-boot-starter", "3.2.0")),
                moduleDependencies   = listOf("core")
            ),
            Module(name = "core")
        ),
        packageOrganisation = PackageOrganisation.BY_FEATURE,
        rootPackages        = listOf("com.example.myapp")
    )
)
```

---

## 5. Consuming the model — reading style source priority

```kotlin
fun highestPrioritySource(codeStyle: CodeStyleInfo): StyleSource? =
    codeStyle.sources.minByOrNull { it.type.priority }
```

Do NOT re-implement the priority order. Always use `StyleSourceType.priority` from the enum.

---

## 6. Running tests

```bash
./gradlew :model:test
```

Tests must complete in under 5 seconds on a developer workstation (SC-005).
