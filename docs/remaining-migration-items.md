# Remaining Migration Items

## Completed

- Maven Java 8 runtime skeleton
- executable shaded jar packaging
- distribution zip packaging
- core DOCX package loading
- document XML parsing
- relationships, styles, numbering, hyperlinks, tables, image traces, summary fields
- CLI options aligned with upstream Node CLI
- CLI usage errors, error stages, verbose diagnostics, stdout behavior, and file output tests for read failures, Markdown, summary, image assets, and manifest
- focused JUnit tests
- upstream `.docx` fixture expected Markdown parity tests
- upstream `.docx` fixture expected summary parity tests
- Node-vs-Java CLI comparison script for CLI metadata, usage errors, verbose diagnostics, Markdown, summary, stdout Markdown, mixed summary/Markdown stdout, debug Markdown, `--include-unsupported-comments` Markdown, manifest, and asset files
- GitHub Release CLI runtime workflow for jar and sources jar assets
- Maven plugin scope decision: remains out of initial straight-conversion scope
- upstream class, CLI, and test mapping documents

## Pending

No active initial straight-conversion items.

## Follow-up Candidates

- Add Maven plugin support only when build-time DOCX to Markdown conversion becomes an explicit Java-side extension request.

## Latest Verification

Run:

```bash
mvn test
scripts/compare-node-java-cli.sh
```

Latest checked on 2026-05-09:

- `mvn test`: 43 tests passed, including expected Markdown and summary parity for 9 upstream fixtures.
- `scripts/compare-node-java-cli.sh`: CLI metadata / usage errors passed, and all 9 upstream fixture verbose diagnostics, Markdown, summary, stdout Markdown, mixed summary/Markdown stdout, debug Markdown, `--include-unsupported-comments` Markdown, manifest, and asset comparisons passed.
- `mvn package`: jar, sources jar, and distribution zip were generated.
