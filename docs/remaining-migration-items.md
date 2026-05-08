# Remaining Migration Items

## Completed

- Maven Java 8 runtime skeleton
- executable shaded jar packaging
- distribution zip packaging
- core DOCX package loading
- document XML parsing
- relationships, styles, numbering, hyperlinks, tables, image traces, summary fields
- CLI options aligned with upstream Node CLI
- focused JUnit tests
- upstream class, CLI, and test mapping documents

## Pending

- Broaden fixture parity against upstream `tests/fixtures/docx/`.
- Add generated-output comparison script for Node-vs-Java Markdown checks.
- Add release workflow after jar naming and smoke commands are finalized.
- Decide Maven plugin follow-up scope.

## Latest Verification

Run:

```bash
mvn test
```
