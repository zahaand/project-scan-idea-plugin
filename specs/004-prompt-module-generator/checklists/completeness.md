# General Completeness Checklist: Prompt — Constitution Prompt Generator Module

**Purpose**: Team planning quality gate — validate all FRs, SCs, assumptions, and edge cases
for gaps, ambiguities, and conflicts before the sprint starts
**Created**: 2026-06-15
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md)
**Audience**: Team (author + reviewers) during sprint planning
**Depth**: Standard (~27 items)

---

## Requirement Completeness

- [x] CHK001 Is the exact preamble wording or invocation syntax of "addressed to `/speckit-constitution`" specified (e.g., must the rendered text open with a literal command reference, or is any form of address acceptable)? [Gap, Spec §FR-002]
  (fixed: FR-002 requires the address within the first 200 characters; exact wording is an implementation choice — confirmed intentional)

- [x] CHK002 Is the minimum required content of the Governance block defined anywhere beyond "standard governance guidance"? The spec and contracts leave the exact wording as an implementation choice — is that intentional and acceptable for a contractual output surface? [Gap, Spec §FR-009, research.md §Finding 6]
  (fixed: FR-009 defines three required elements verifiable by presence — semver policy, changelog convention, amendment procedure — exact phrasing is implementation choice, confirmed intentional)

- [x] CHK003 Is the minimum required phrasing for "explicit conflict-resolution language" specified, or may any wording that conveys the precedence hierarchy satisfy FR-005? [Clarity, Spec §FR-005]
  (fixed: any wording conveying the precedence hierarchy satisfies FR-005; US2-scenario-1 test verifies "take precedence" or equivalent)

- [x] CHK004 Is the exact marker phrase for unavailable data contractually fixed ("not detected" vs "not available" vs any equivalent), or may the generator choose freely? [Clarity, Spec §FR-008]
  (fixed: FR-008 contractually fixes both markers; updated in contracts/prompt-api.md §Unavailable-Data Markers table)

- [x] CHK005 Are separator conventions in `ConstitutionPrompt.render()` — blank lines between blocks, between `###` groups, between `####` sub-sections — specified in FR-012 or contracts, or left to implementation discretion? [Completeness, Spec §FR-012, contracts/prompt-api.md]
  (fixed: FR-012 §Separator Conventions and contracts/prompt-api.md §Separator Conventions specify all separator rules explicitly)

- [x] CHK006 When both mandatory and advisory sub-sections of the `"project standard"` group would be empty (zero active linter rules), is the full rendering defined — only `emptyNotation` with no `####` headings emitted at all? [Clarity, Spec §FR-006]
  (fixed: FR-006 explicitly specifies "NO #### Mandatory and NO #### Advisory headings are emitted" when activeRules is empty; scenario 6 (C1) in PromptGeneratorEmptyModelTest verifies)

- [x] CHK007 Is `languageLevel = ""` (empty string) explicitly listed in the spec's no-filtering triggers, or only in data-model.md's table? [Completeness, Spec §FR-007, data-model.md §Language-Level Filtering Algorithm]
  (fixed: FR-007 explicitly lists "" as a no-filtering trigger; SC-004(e) and US3-scenario coverage in T019 verify it)

---

## Requirement Clarity

- [x] CHK008 Does FR-007's extraction algorithm explicitly cover strings with leading whitespace (e.g., `" 11"`)? `trimStart()` handles it in practice, but the spec's example set omits this case. [Clarity, Spec §FR-007]
  (fixed: FR-007 documents trimStart() in the extraction algorithm; SC-004(f) and US3-scenario-8 cover " 11" explicitly)

- [x] CHK009 Is it specified whether the extracted leading integer must be a recognized Java LTS release (8, 11, 17, 21) or whether any extracted integer (e.g., `"9"` → 9, `"16"` → 16) is used as-is for comparison? [Clarity, Spec §Edge Cases]
  (fixed: spec §Edge Cases explicitly states any extracted integer is used as-is; not restricted to LTS values)

- [x] CHK010 Is the distinction between the `"baseline quality requirement"` group's flat `-` bullet list and the `"project standard"` group's `####` sub-section split explicitly stated in FR-012 or the contracts, or only implied by the absence of `####` headings under that group? [Clarity, Spec §FR-012, contracts/prompt-api.md §Rendered Markdown Structure]
  (fixed: FR-012 and contracts/prompt-api.md §Heading level rules both explicitly state baseline group is always a flat list with NO #### sub-sections)

- [x] CHK011 Is the prohibition boundary in FR-001 ("no IntelliJ Platform API") defined precisely enough to cover transitive class loading — e.g., a `:model` type that happens to implement an IntelliJ interface — or does it only govern direct call-sites in `:prompt`? [Clarity, Spec §FR-001, §SC-006]
  (fixed: FR-001 explicitly covers transitive presence "including via a :model or :baseline type"; T027 enforces via gradle dependency inspection)

- [x] CHK012 Is the exact mapping from `Obligation` enum values (`MUST`, `SHOULD`) to rendered rule wording defined? The spec states the generator reads `obligation` for wording but does not specify the output text. [Gap, Spec §Assumptions, data-model.md §Consumed Types from :baseline]
  (fixed: spec §Assumptions defines MUST→"MUST" and SHOULD→"SHOULD" explicitly; obligation marker is required in every baseline bullet)

---

## Requirement Consistency & Conflicts

- [x] CHK013 Does "MUST NOT suppress [the cause] silently" in the edge-case section conflict with "MAY include the cause string" in FR-008? One clause is obligatory, the other permissive — is there a definitive ruling? [Conflict, Spec §FR-008 / §Edge Cases]
  (fixed: FR-008 resolved to MUST include non-null cause in notation; "MUST NOT suppress" and "MUST include" are now consistent — no conflict; US4-scenario-5 verifies)

- [x] CHK014 Does FR-004's rule that "every individual rule emitted MUST be associated with its group's origin tag" apply to both the rendered Markdown output and the intermediate `OriginGroup` structure, or only to the final rendered string? [Clarity, Consistency, Spec §FR-004]
  (fixed: FR-004 explicitly scopes to "the rendered Markdown output … this is a guarantee about the observable rendered string, not about the intermediate OriginGroup structure")

- [x] CHK015 Is the notation for the empty `"project standard"` group (spec edge cases: "notes that no active linter rules were found") consistent with the empty `"baseline quality requirement"` group notation? Are both empty-group descriptions specified at the same level of detail? [Consistency, Spec §Edge Cases, data-model.md §OriginGroup]
  (fixed: contracts/prompt-api.md §Empty-Group Notations table updated (I1 fix in last session); both groups specified at equal precision; both verified by PromptGeneratorEmptyModelTest scenarios 6 and 7)

---

## Acceptance Criteria Quality

- [x] CHK016 Does SC-001's "any valid combination of inputs" define what constitutes a valid vs invalid input? Specifically, is a `null` `ScanResult` parameter in scope or out of scope for the no-exception guarantee? [Completeness, Spec §SC-001]
  (fixed: SC-001 and spec §Assumptions explicitly clarify null inputs are out of scope, enforced by Kotlin's type system at compile time)

- [x] CHK017 Does SC-004 cover the `languageLevel = ""` (empty string) case? It lists four boundary cases (null, "17.0.1", "unknown", and one numeric) but does not explicitly include empty string. [Completeness, Spec §SC-004]
  (fixed: SC-004 case (e) added explicitly for languageLevel = ""; covered in T019)

- [x] CHK018 The plan specifies a performance goal of "<10 ms per call" — is this measurable by any SC or test requirement in spec.md, or is it an unverified non-functional requirement with no acceptance criterion? [Measurability, plan.md §Technical Context]
  (confirmed: plan.md explicitly marks "<10ms" as informational with no SC; not a verifiable requirement by design — pure in-memory transformation with no I/O)

- [x] CHK019 FR-011 excludes `LightPlatformTestCase` and `BasePlatformTestCase` by name — are other IntelliJ test base classes (`IdeaTestFixtureFactory`, `ProjectBuilder`, etc.) equally prohibited, or only the two named? [Completeness, Spec §FR-011]
  (fixed: FR-011 clarified to "no IntelliJ Platform test infrastructure of any kind"; the two named classes are examples only, not an exhaustive list)

- [x] CHK020 SC-002 verifies 100% origin-tag coverage by parsing Markdown — is the parsing method specified precisely enough (e.g., every `-` bullet under `## Core Principles` must follow a `###` heading matching `"project standard"` or `"baseline quality requirement"`) to yield deterministic test code? [Measurability, Spec §SC-002]
  (fixed: SC-002 specifies the exact parsing rule; implemented deterministically in PromptGeneratorPriorityHierarchyTest scenario 3)

---

## Scenario Coverage

- [x] CHK021 Is there a US3 acceptance scenario for `languageLevel` strings that contain digits followed immediately by a non-separator non-digit (e.g., `"11a"`)? `takeWhile { isDigit() }` would extract `11`, but this case is absent from the spec's example set. [Coverage, Spec §US3]
  (fixed: US3 scenario 9 and SC-004(g) added to cover digits-then-non-digit case; tested in T019)

- [x] CHK022 Is the case where `SectionResult.Error` carries `cause = null` (error present but no diagnostic message) explicitly covered in US4 or the edge cases, and is the required rendered output defined? [Coverage, Spec §US4 / §Edge Cases]
  (fixed: spec §Edge Cases and FR-008 explicitly define cause=null → plain "not available", never "(cause: null)"; US4-scenario-5 and T022 verify)

- [x] CHK023 Is the `"baseline quality requirement"` group's empty-baseline rendering (edge case: "notes that no baseline rules are available") specified with the same precision as the `"project standard"` group's empty-linter-rules case? [Consistency, Coverage, Spec §Edge Cases]
  (fixed: contracts/prompt-api.md §Empty-Group Notations table specifies both groups at equal precision; scenario 7 (C1) in PromptGeneratorEmptyModelTest verifies empty baseline)

- [x] CHK024 Do US1's acceptance scenario 2 (five linter rules tagged "project standard") and US2's acceptance scenario 3 (no rule without an origin tag) overlap in what they validate? If so, is the test boundary between `PromptGeneratorFullModelTest` and `PromptGeneratorPriorityHierarchyTest` clearly delineated in the spec? [Clarity, Spec §US1 / §US2]
  (confirmed: US1-sc2 verifies that rules appear under the correct ### label; US2-sc3 verifies structural coverage — no bullet appears outside a valid ### heading; distinct assertions, separate test classes)

---

## Dependencies & Assumptions

- [x] CHK025 Is there a verification step — in plan.md gates, tasks.md, or an explicit assumption — that confirms `:model` and `:baseline` types carry no transitive IntelliJ Platform dependency before `:prompt` implementation begins? research.md Finding 4 verified this at research time, but not as a gated prerequisite. [Dependency, Spec §Assumptions, research.md §Finding 4]
  (fixed: T000 is an explicit BLOCKER gate in tasks.md; completed and verified at sprint start before any implementation task)

- [x] CHK026 Is the caller's responsibility for input validation documented — must the caller guarantee a non-null `List<BaselineRule>` and non-null `ScanResult`, or must the generator handle `null` arguments gracefully? [Completeness, Spec §FR-001 / §Out of Scope]
  (fixed: spec §Assumptions and SC-001 explicitly document caller responsibility for non-null inputs; Kotlin's type system enforces this at compile time)

- [x] CHK027 Is there a mechanism specified to re-validate the "no IntelliJ Platform dependency in :model/:baseline" assumption if either upstream module adds a platform dependency in a future sprint? [Assumption, Spec §Assumptions]
  (deferred: Sprint 6 constitution amendment will add a standing platform-cleanliness gate to the Constitution Check gate; T027 verification pattern (./gradlew :prompt:dependencies) is the current per-sprint enforcement mechanism)

---

## Notes

- Check items off as completed: `[x]`
- **[Conflict]** items require a decision before sprint start — ambiguous requirements produce divergent implementations
- **[Gap]** items may be intentionally left as implementation choices; confirm and document that intent
- **[Assumption]** items should be validated before T001 begins to avoid mid-sprint blockers
- Most **[Clarity]** items can be resolved by adding one sentence to spec.md or contracts/prompt-api.md
