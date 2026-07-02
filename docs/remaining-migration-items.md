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
- YAML front matter CLI parity, including `--front-matter include|exclude`
- Word reviewing comment footnote parity
- Word tracked insertion / deletion markup parity
- GitHub Release CLI runtime workflow for jar and sources jar assets
- Maven plugin module with `convert` and `convert-directory` goals, later
  separated to <https://github.com/igapyon/miku-docx2md-java-maven>
- Maven plugin full-coordinate smoke script, later moved to the separated Maven
  plugin repository
- Single Maven project layout after Maven plugin separation
- CLI batch conversion for multiple positional files and `--input-directory`
- Runtime file/batch conversion API for CLI and separated Maven plugin adapter reuse
- upstream class, CLI, and test mapping documents

## Pending

No active initial straight-conversion items.

## Follow-up Candidates

- Keep separated Maven plugin compatibility aligned when runtime releases move.
- Update `miku-docx2md-java-maven` to call the runtime file/batch conversion API
  instead of keeping duplicate directory conversion logic.

## Latest Verification

Run:

```bash
mvn test
scripts/compare-node-java-cli.sh
mvn package
```

Latest checked on 2026-07-02 after following upstream `devel-tiga0702vfd`
tag `v1.2.1`, including front matter output, `sectPr` summary handling, Word
comment footnotes, tracked insertion / deletion markup, and version number
alignment:

- `mvn test`: 65 runtime / CLI tests passed, including expected Markdown and
  summary parity for 11 upstream fixtures.
- `scripts/compare-node-java-cli.sh`: CLI metadata / usage errors passed, and all 11 upstream fixture verbose diagnostics, Markdown, summary, stdout Markdown, mixed summary/Markdown stdout, debug Markdown, `--include-unsupported-comments` Markdown, manifest, and asset comparisons passed.
- `mvn package`: runtime jar, runtime sources jar, and distribution zip were
  generated.
