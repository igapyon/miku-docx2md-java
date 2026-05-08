# Development

## Commands

```bash
mvn test
mvn package
sh scripts/smoke-maven-plugin.sh
java -jar miku-docx2md/target/miku-docx2md-0.9.0.jar --version
scripts/compare-node-java-cli.sh
```

## Release Assets

`.github/workflows/release-cli-runtime.yml` builds the Maven package from a
`v*` tag or manual `tag_name`, verifies the runtime jar with Java 8 using
`--version`, and uploads the runtime jar plus sources jar from the
`miku-docx2md` runtime module to the GitHub Release.

## Maven Plugin Scope

Maven plugin support is now an explicit Java-side extension aligned with the
closest sister project `workplace/miku-xlsx2md-java-devel`. The plugin module
provides `convert` and `convert-directory` goals.

Focused tests:

```bash
mvn test -Dtest=MikuDocx2mdCoreTest
mvn test -Dtest=MikuDocx2mdCliTest
mvn test -Dtest=MikuDocx2mdFixtureParityTest
mvn -pl miku-docx2md-maven-plugin -am -Dtest=MikuDocx2mdMojoTest test
sh scripts/smoke-maven-plugin.sh
```

`scripts/smoke-maven-plugin.sh` installs the local reactor artifacts and runs
the plugin by full coordinate for both `convert` and `convert-directory`.

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
