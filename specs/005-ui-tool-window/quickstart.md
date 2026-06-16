# Quickstart: UI Tool Window

**Feature**: `005-ui-tool-window`

## Running the plugin in a sandboxed IDE

```bash
# From repo root
./gradlew runIde
```

This launches a sandboxed IntelliJ IDEA instance with the plugin loaded. Open any JVM project (Maven or Gradle) in the sandbox IDE to exercise the scanner.

## Verifying the tool window

1. In the sandboxed IDE, open **View → Tool Windows → Project Scan**.
2. Confirm the panel shows only the "Scan" button and hint text (pre-scan state).
3. Click **Scan**. Confirm the button disables immediately.
4. Wait for the scan to complete. Confirm six collapsible sections appear in order:
   Tech Stack → Code Style → Linters → Tests → Project Structure → Constitution Prompt.
5. Confirm the first five sections are expanded and the Constitution Prompt is collapsed.
6. Click **Copy** on any `Ok` section. Paste into a text editor and verify the text matches.
7. Click **Scan** a second time. Confirm the existing sections remain visible during the scan and are replaced only on completion. Confirm the collapse state resets to the default (collection sections expanded, Constitution Prompt collapsed). *(Covers FR-019, FR-020)*

## Key source locations after Sprint 5

| Purpose | Path |
|---------|------|
| Tool Window Factory | `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanToolWindowFactory.kt` |
| Main Panel | `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanPanel.kt` |
| Section Panel | `src/main/kotlin/dev/zahaand/projectscan/ui/SectionPanel.kt` |
| Section Renderer | `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt` |
| Message Bundle | `src/main/resources/messages/ProjectScanBundle.properties` |
| Plugin descriptor | `src/main/resources/META-INF/plugin.xml` |

## Running static analysis

```bash
./gradlew detekt ktlintCheck
```

Both must pass with zero violations before submitting for review.
