# Quickstart: Using `ScanService`

**Audience**: Developers wiring the scan layer into the plugin (Sprint 4+ consumers).

---

## Dependency

The `:scan` module depends on `:model`. Consumers of the scan result depend only on `:model`
(they import `ScanResult` from there, not from `:scan`).

```kotlin
// In the consumer's build.gradle.kts:
implementation(project(":model"))
// Only the scan trigger / plugin entry point needs:
implementation(project(":scan"))
```

---

## Running a Scan

`ScanService` is a plain class. Instantiate it with production adapters (from the plugin entry
point) or with fakes (in tests).

```kotlin
// Production wiring (inside a plugin service or action):
val scanService = ScanService(
    buildSystemPort     = IjBuildSystemAdapter(project),
    dependencyPort      = IjDependencyAdapter(project),
    styleSourcePort     = IjStyleSourceAdapter(project),
    linterPort          = IjLinterAdapter(project),
    linterConfigParser  = CompositeLinterConfigParser(),  // delegates to Checkstyle/PMD parsers
    testInfoPort        = IjTestInfoAdapter(project),
    moduleStructurePort = IjModuleStructureAdapter(project),
)

val result: ScanResult = scanService.scan()
```

---

## Working with `ScanResult`

Each section is a `SectionResult<T>` — handle all three cases:

```kotlin
when (val stack = result.stack) {
    is SectionResult.Ok    -> useStack(stack.data)
    is SectionResult.Empty -> showNoStackData()
    is SectionResult.Error -> logError(stack.cause)
}
```

Kotlin exhaustive `when` requires no `else` branch.

---

## Unit Testing with Fakes

```kotlin
val fakeDependencyPort = FakeDependencyPort(
    moduleMap = mapOf(
        "app" to listOf(
            Dependency("org.springframework.boot", "spring-boot-starter", "3.3.0"),
        )
    )
)

val collector = StackCollector(
    dependencyPort  = fakeDependencyPort,
    buildSystemPort = FakeBuildSystemPort(BuildSystem.MAVEN, mapOf("app" to "17")),
)

val result = collector.collect()
assertThat(result).isInstanceOf(SectionResult.Ok::class.java)
```

No IntelliJ fixtures, no `LightPlatformTestCase` — pure JUnit 5.
