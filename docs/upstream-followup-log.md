# Upstream Follow-up Log

## 2026-05-08

- Started Java straight conversion from <https://github.com/igapyon/miku-docx2md>.
- Used local upstream checkout `../miku-docx2md` at `tiga0508wfj` / `0667817331d617b5c8eec5a529a6b430a4d7b91f`.
- Used <https://github.com/igapyon/miku-xlsx2md-java> via `../miku-xlsx2md-java` as the same-layer Java sister reference.
- Implemented initial Java runtime core, CLI, Maven skeleton, mapping docs, and focused JUnit tests.
- Kept Maven plugin support out of initial scope.

## 2026-05-09

- Copied upstream `.docx` fixture corpus into Java test resources for standalone regression coverage.
- Added `MikuDocx2mdFixtureParityTest` for fixture parsing, summary field stability, and image asset counts.
- Added `scripts/compare-node-java-cli.sh` for generated Node-vs-Java CLI comparison output under `workplace/`.
- Verified `mvn test` and Node-vs-Java comparison across all 9 copied upstream fixtures.
- Promoted fixture smoke parity to expected Markdown parity by tracking Node CLI Markdown under `src/test/resources/expected/markdown/`.
- Added expected summary parity by tracking Node runtime summary text under `src/test/resources/expected/summary/`.
- Added GitHub Release CLI runtime workflow from the miku-soft Java straight-conversion template.
- Closed the initial Maven plugin scope decision by keeping plugin support out of initial straight conversion.
- Added CLI file-output tests for Markdown, summary, image assets, manifest, and verbose stderr behavior.
- Expanded Node-vs-Java generated comparison to cover both Markdown and summary CLI outputs.
- Aligned Java asset manifest JSON with the upstream Node manifest contract and expanded generated comparison to cover manifests and asset files.
- Aligned Java CLI I/O error stage labels with the upstream CLI and added read-failure regression coverage.
- Added write-failure regression coverage for markdown, summary, and asset output stages.
- Aligned missing-input CLI behavior with upstream: print help and return exit code `1`.
- Aligned Java verbose diagnostic keys with the upstream CLI and strengthened CLI verbose regression coverage.
- Added CLI usage-error regressions for unknown options, missing option values, and multiple input files.
- Aligned Markdown stdout behavior with upstream raw output by removing the Java-only trailing newline and adding stdout summary/Markdown ordering regressions.
- Expanded Node-vs-Java generated comparison to cover stdout Markdown and mixed summary/Markdown stdout output.
- Expanded Java CLI help text to carry the upstream detailed contract, options, outputs, examples, and exit-code descriptions with Java commands.
- Expanded Node-vs-Java comparison to cover CLI metadata output and representative usage-error stdout / stderr / exit status.
- Aligned Java help trailing newline behavior with the upstream CLI help output.
- Expanded Node-vs-Java fixture comparison to cover `--debug` and `--include-unsupported-comments` Markdown outputs.
- Expanded Node-vs-Java fixture comparison to cover `--verbose` stderr diagnostics after normalizing elapsed times and comparison work directories.
