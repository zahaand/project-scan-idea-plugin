# General Completeness Checklist: Prompt — Constitution Prompt Generator Module

**Purpose**: Team planning quality gate — validate all FRs, SCs, assumptions, and edge cases
for gaps, ambiguities, and conflicts before the sprint starts
**Created**: 2026-06-15
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md)
**Audience**: Team (author + reviewers) during sprint planning
**Depth**: Standard (~27 items)

---

## Requirement Completeness

- [ ] CHK001 Is the exact preamble wording or invocation syntax of "addressed to `/speckit-constitution`" specified (e.g., must the rendered text open with a literal command reference, or is any form of address acceptable)? [Gap, Spec §FR-002]
- [ ] CHK002 Is the minimum required content of the Governance block defined anywhere beyond "standard governance guidance"? The spec and contracts leave the exact wording as an implementation choice — is that intentional and acceptable for a contractual output surface? [Gap, Spec §FR-009, research.md §Finding 6]
- [ ] CHK003 Is the minimum required phrasing for "explicit conflict-resolution language" specified, or may any wording that conveys the precedence hierarchy satisfy FR-005? [Clarity, Spec §FR-005]
- [ ] CHK004 Is the exact marker phrase for unavailable data contractually fixed ("not detected" vs "not available" vs any equivalent), or may the generator choose freely? [Clarity, Spec §FR-008]
- [ ] CHK005 Are separator conventions in `ConstitutionPrompt.render()` — blank lines between blocks, between `###` groups, between `####` sub-sections — specified in FR-012 or contracts, or left to implementation discretion? [Completeness, Spec §FR-012, contracts/prompt-api.md]
- [ ] CHK006 When both mandatory and advisory sub-sections of the `"project standard"` group would be empty (zero active linter rules), is the full rendering defined — only `emptyNotation` with no `####` headings emitted at all? [Clarity, Spec §FR-006]
- [ ] CHK007 Is `languageLevel = ""` (empty string) explicitly listed in the spec's no-filtering triggers, or only in data-model.md's table? [Completeness, Spec §FR-007, data-model.md §Language-Level Filtering Algorithm]

---

## Requirement Clarity

- [ ] CHK008 Does FR-007's extraction algorithm explicitly cover strings with leading whitespace (e.g., `" 11"`)? `trimStart()` handles it in practice, but the spec's example set omits this case. [Clarity, Spec §FR-007]
- [ ] CHK009 Is it specified whether the extracted leading integer must be a recognized Java LTS release (8, 11, 17, 21) or whether any extracted integer (e.g., `"9"` → 9, `"16"` → 16) is used as-is for comparison? [Clarity, Spec §Edge Cases]
- [ ] CHK010 Is the distinction between the `"baseline quality requirement"` group's flat `-` bullet list and the `"project standard"` group's `####` sub-section split explicitly stated in FR-012 or the contracts, or only implied by the absence of `####` headings under that group? [Clarity, Spec §FR-012, contracts/prompt-api.md §Rendered Markdown Structure]
- [ ] CHK011 Is the prohibition boundary in FR-001 ("no IntelliJ Platform API") defined precisely enough to cover transitive class loading — e.g., a `:model` type that happens to implement an IntelliJ interface — or does it only govern direct call-sites in `:prompt`? [Clarity, Spec §FR-001, §SC-006]
- [ ] CHK012 Is the exact mapping from `Obligation` enum values (`MUST`, `SHOULD`) to rendered rule wording defined? The spec states the generator reads `obligation` for wording but does not specify the output text. [Gap, Spec §Assumptions, data-model.md §Consumed Types from :baseline]

---

## Requirement Consistency & Conflicts

- [ ] CHK013 Does "MUST NOT suppress [the cause] silently" in the edge-case section conflict with "MAY include the cause string" in FR-008? One clause is obligatory, the other permissive — is there a definitive ruling? [Conflict, Spec §FR-008 / §Edge Cases]
- [ ] CHK014 Does FR-004's rule that "every individual rule emitted MUST be associated with its group's origin tag" apply to both the rendered Markdown output and the intermediate `OriginGroup` structure, or only to the final rendered string? [Clarity, Consistency, Spec §FR-004]
- [ ] CHK015 Is the notation for the empty `"project standard"` group (spec edge cases: "notes that no active linter rules were found") consistent with the empty `"baseline quality requirement"` group notation? Are both empty-group descriptions specified at the same level of detail? [Consistency, Spec §Edge Cases, data-model.md §OriginGroup]

---

## Acceptance Criteria Quality

- [ ] CHK016 Does SC-001's "any valid combination of inputs" define what constitutes a valid vs invalid input? Specifically, is a `null` `ScanResult` parameter in scope or out of scope for the no-exception guarantee? [Completeness, Spec §SC-001]
- [ ] CHK017 Does SC-004 cover the `languageLevel = ""` (empty string) case? It lists four boundary cases (null, "17.0.1", "unknown", and one numeric) but does not explicitly include empty string. [Completeness, Spec §SC-004]
- [ ] CHK018 The plan specifies a performance goal of "<10 ms per call" — is this measurable by any SC or test requirement in spec.md, or is it an unverified non-functional requirement with no acceptance criterion? [Measurability, plan.md §Technical Context]
- [ ] CHK019 FR-011 excludes `LightPlatformTestCase` and `BasePlatformTestCase` by name — are other IntelliJ test base classes (`IdeaTestFixtureFactory`, `ProjectBuilder`, etc.) equally prohibited, or only the two named? [Completeness, Spec §FR-011]
- [ ] CHK020 SC-002 verifies 100% origin-tag coverage by parsing Markdown — is the parsing method specified precisely enough (e.g., every `-` bullet under `## Core Principles` must follow a `###` heading matching `"project standard"` or `"baseline quality requirement"`) to yield deterministic test code? [Measurability, Spec §SC-002]

---

## Scenario Coverage

- [ ] CHK021 Is there a US3 acceptance scenario for `languageLevel` strings that contain digits followed immediately by a non-separator non-digit (e.g., `"11a"`)? `takeWhile { isDigit() }` would extract `11`, but this case is absent from the spec's example set. [Coverage, Spec §US3]
- [ ] CHK022 Is the case where `SectionResult.Error` carries `cause = null` (error present but no diagnostic message) explicitly covered in US4 or the edge cases, and is the required rendered output defined? [Coverage, Spec §US4 / §Edge Cases]
- [ ] CHK023 Is the `"baseline quality requirement"` group's empty-baseline rendering (edge case: "notes that no baseline rules are available") specified with the same precision as the `"project standard"` group's empty-linter-rules case? [Consistency, Coverage, Spec §Edge Cases]
- [ ] CHK024 Do US1's acceptance scenario 2 (five linter rules tagged "project standard") and US2's acceptance scenario 3 (no rule without an origin tag) overlap in what they validate? If so, is the test boundary between `PromptGeneratorFullModelTest` and `PromptGeneratorPriorityHierarchyTest` clearly delineated in the spec? [Clarity, Spec §US1 / §US2]

---

## Dependencies & Assumptions

- [ ] CHK025 Is there a verification step — in plan.md gates, tasks.md, or an explicit assumption — that confirms `:model` and `:baseline` types carry no transitive IntelliJ Platform dependency before `:prompt` implementation begins? research.md Finding 4 verified this at research time, but not as a gated prerequisite. [Dependency, Spec §Assumptions, research.md §Finding 4]
- [ ] CHK026 Is the caller's responsibility for input validation documented — must the caller guarantee a non-null `List<BaselineRule>` and non-null `ScanResult`, or must the generator handle `null` arguments gracefully? [Completeness, Spec §FR-001 / §Out of Scope]
- [ ] CHK027 Is there a mechanism specified to re-validate the "no IntelliJ Platform dependency in :model/:baseline" assumption if either upstream module adds a platform dependency in a future sprint? [Assumption, Spec §Assumptions]

---

## Notes

- Check items off as completed: `[x]`
- **[Conflict]** items require a decision before sprint start — ambiguous requirements produce divergent implementations
- **[Gap]** items may be intentionally left as implementation choices; confirm and document that intent
- **[Assumption]** items should be validated before T001 begins to avoid mid-sprint blockers
- Most **[Clarity]** items can be resolved by adding one sentence to spec.md or contracts/prompt-api.md
