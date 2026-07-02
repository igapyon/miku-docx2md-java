# Development

## Commands

```bash
mvn test
mvn package
java -jar target/miku-docx2md-1.2.1.jar --version
scripts/compare-node-java-cli.sh
```

## Release Assets

`.github/workflows/release-cli-runtime.yml` builds the Maven package from a
`v*` tag or manual `tag_name`, verifies the runtime jar with Java 8 using
`--version`, and uploads the runtime jar plus sources jar from the
single `miku-docx2md` Maven project to the GitHub Release.

## Maven Plugin Scope

Maven plugin support has been separated into
<https://github.com/igapyon/miku-docx2md-java-maven>. Keep this repository
focused on the runtime, CLI, core API, fixtures, and distribution artifacts.
The separated Maven plugin repository owns plugin parameters, goals, examples,
and plugin smoke verification.

Runtime file and batch conversion behavior is exposed through
`jp.igapyon.mikudocx2md.core.MikuDocx2mdFileConverter` and its options/result
classes so CLI and separated build-tool adapters can share file I/O, asset,
summary, and directory traversal behavior.

Focused tests:

```bash
mvn test -Dtest=MikuDocx2mdCoreTest
mvn test -Dtest=MikuDocx2mdFileConverterTest
mvn test -Dtest=MikuDocx2mdCliTest
mvn test -Dtest=MikuDocx2mdFixtureParityTest
```

## Local Workspace

Use `workplace/` for temporary upstream clones, generated comparison output,
and extracted archives. Only `workplace/.gitkeep` is tracked.

`scripts/compare-node-java-cli.sh` writes Node-vs-Java CLI metadata, usage
error output, verbose diagnostics, generated Markdown, summary files, stdout
Markdown, mixed summary/Markdown stdout, debug Markdown,
`--include-unsupported-comments` Markdown, asset directories, manifests, and
diffs under `workplace/node-java-cli/`.

## Upstream Maintenance Flow

1. Check the upstream snapshot or current upstream diff.
2. Use `docs/upstream-class-mapping.md` to locate Java classes.
3. Use `docs/upstream-test-mapping.md` to locate focused Java tests.
4. Apply the smallest Java change that preserves upstream behavior.
5. Record the check in `docs/upstream-followup-log.md`.
