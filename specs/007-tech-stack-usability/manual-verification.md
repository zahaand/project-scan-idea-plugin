# Manual Verification Checklist — Sprint 7

**Project**: seed-farming-service monorepo (130+ modules, ~250 resolved dependencies)  
**How to run**: Open the monorepo in IntelliJ, trigger a full project scan, inspect both the generated LLM prompt and the UI tool window output.

---

## Checklist

- [ ] **SC-001** — Tech Stack section is ≤ 40 dependency-entry lines. Count only the dependency lines (`-` entries for coordinates); exclude preamble lines (Build System, JDK Version, Language Level) from the count.

- [ ] **SC-002** — None of the known transitive-only artifacts appear anywhere in the Tech Stack or Testing sections:
  `asm`, `objenesis`, `listenablefuture`, `failureaccess`, `j2objc-annotations`, `checker-qual`, `aopalliance`, `paranamer`

- [ ] **SC-001 / FR-008 / FR-009** — Inverted view format is correct:
  - Uniform (single-version) entry: `- groupId:artifactId:version [N modules]` — no module names listed
  - Multi-version entry: coordinate header line (`- groupId:artifactId`) followed by indented lines (`  - version  aggregatorName: module1, module2`); named aggregators in alphabetical order, null-aggregator group last

- [ ] **US1 scenario 2** — `testcontainers` (known multi-version dep: `1.19.8` / `1.20.0` / `1.20.1`) renders as a multi-version entry with carrier modules grouped by aggregator, each version on its own indented line.

- [ ] **SC-003** — Testing section contains **no** `Frameworks:` header and **no** framework name entries (JUnit, Mockito, Testcontainers, AssertJ, etc.). Section contains exactly:
  - Coverage threshold: `not detected` (JaCoCo is absent from this project)
  - Test source roots (compacted)
  - Naming pattern (if present)

- [ ] **SC-006** — No `Project Structure` section appears anywhere in the generated prompt or UI tool window output. No `rootPackages` or `secondLevelSegments` field or value appears anywhere.

- [ ] **SC-005** — Tech Stack and Testing content is **byte-identical** between the generated LLM prompt (copy the prompt text) and the UI tool window (copy the tool window text) for the same scan. Compare character-by-character; any difference is a regression.

---

## Automated coverage summary (for reference)

| SC | Automated | Test location |
|----|-----------|---------------|
| SC-001 (≤40 lines) | ✗ Manual only | — |
| SC-002 (no transitives) | ✓ Partial | `OutputFormattersTest.kt` — direct-only input set; adapter-layer exclusion confirmed manually |
| SC-003 (no Frameworks header) | ✗ Manual only | — |
| SC-004 (inline discrepancies) | ✓ Code inspection | No standalone block in `OutputFormatters.kt` |
| SC-005 (byte-identical) | ✓ Partial | `ScanResultRendererSmokeTest.kt#SC-005 byte-identical` — Tech Stack; Testing parity is C1 deviation (Sprint 9) |
| SC-006 (no Project Structure) | ✗ Manual only | — |
| SC-007 (resolved version shown) | ✓ Partial | `OutputFormattersTest.kt#SC-007`; live-IDE confirmation is this checklist |
