# Quickstart: Prompt — Constitution Prompt Generator Module

**Feature**: `004-prompt-module-generator` | **Date**: 2026-06-15

---

## Prerequisites

- JDK 21 (JetBrains Runtime or compatible)
- Gradle wrapper (`./gradlew`) — already in the repo root
- `:model` and `:baseline` submodules — already implemented

---

## 1. Register the new submodule

Add to `settings.gradle.kts`:

```kotlin
include(":prompt")
```

---

## 2. Create the build file

Create `prompt/build.gradle.kts` (mirrors `:baseline`, no IntelliJ Platform):

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":model"))
    implementation(project(":baseline"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.files("config/detekt.yml"))
    buildUponDefaultConfig = true
}
```

---

## 3. Create the source tree

```
mkdir -p prompt/src/main/kotlin/dev/zahaand/projectscan/prompt
mkdir -p prompt/src/test/kotlin/dev/zahaand/projectscan/prompt
```

---

## 4. Minimal usage example

```kotlin
import dev.zahaand.projectscan.model.*
import dev.zahaand.projectscan.baseline.BaselineRuleProvider
import dev.zahaand.projectscan.prompt.PromptGenerator

val scan = ScanResult(
    stack   = SectionResult.Ok(StackInfo(languageLevel = "17", buildSystem = BuildSystem.GRADLE)),
    codeStyle = SectionResult.Empty,
    linters = SectionResult.Empty,
    tests   = SectionResult.Empty,
    structure = SectionResult.Empty,
)

val baseline = BaselineRuleProvider().loadRules()
val prompt   = PromptGenerator().generate(scan, baseline)

println(prompt.render())
```

---

## 5. Run the tests

```bash
./gradlew :prompt:test
```

All tests are pure JVM — no IntelliJ Platform environment required, no IDE startup.

---

## 6. Run detekt + ktlint

```bash
./gradlew :prompt:detekt
./gradlew :prompt:ktlintCheck
```

---

## Key constraints (for future contributors)

- **No IntelliJ Platform API** — any import from `com.intellij.*` is a build violation.
- **No file I/O** — the generator reads only its two arguments; no `File`, `Path`, or I/O calls.
- **No clipboard access** — writing to clipboard belongs to the `:ui` module (Sprint 5).
- **Deterministic output** — no `System.currentTimeMillis()`, `UUID`, or random in render output.
- **Nothing depends on `:prompt`** — it is a leaf module; adding dependents must be a deliberate
  constitution amendment.
