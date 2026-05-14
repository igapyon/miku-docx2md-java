# Maven Plugin Separation Worklog

## 2026-05-14

Separated the Maven plugin adapter from `miku-docx2md-java` into:

<https://github.com/igapyon/miku-docx2md-java-maven>

## Source Repository Changes

- Removed `miku-docx2md-maven-plugin` from the root Maven reactor.
- Kept this repository focused on `miku-docx2md`, the Java runtime / CLI
  project.
- Removed the root POM properties and plugin management entries that were only
  needed by the Maven plugin module.
- Flattened the remaining runtime module so this repository is a single Maven
  jar project.
- Moved runtime sources and tests from `miku-docx2md/src/` to repository-root
  `src/`.
- Merged the former runtime module POM into root `pom.xml`; the root project is
  now the `jp.igapyon:miku-docx2md` jar artifact instead of an aggregator POM.
- Updated runtime artifact paths from `miku-docx2md/target/...` to
  `target/...`.
- Updated the release workflow and CLI comparison script for the root-level
  `target/` and `src/test/resources/` layout.
- Removed Maven plugin invocation examples and smoke-test instructions from
  the runtime README and development notes.
- Updated mapping/status documents so Maven plugin tests and smoke checks are
  treated as separated-repository concerns.

## Git Diff Shape

Before staging, Git reports the single-project flattening as deletions under
`miku-docx2md/src/...` and additions under root `src/...`. This is expected.
After staging the old and new paths together, Git can detect the source and
test files as moves/renames.

The intentionally deleted paths are:

- `miku-docx2md-maven-plugin/`
- `miku-docx2md/pom.xml`
- `scripts/smoke-maven-plugin.sh`

The intentionally moved paths are:

- `miku-docx2md/src/main/...` to `src/main/...`
- `miku-docx2md/src/test/...` to `src/test/...`
- `miku-docx2md/src/assembly/dist.xml` to `src/assembly/dist.xml`

## Runtime Verification

After the separation and flattening, this repository was verified with:

```bash
mvn test
mvn package
scripts/compare-node-java-cli.sh
```

The expected runtime outputs are now:

- `target/miku-docx2md-1.0.0.jar`
- `target/miku-docx2md-1.0.0-sources.jar`
- `target/miku-docx2md-1.0.0-dist.zip`

## Separated Repository Shape

The separated repository owns:

- Maven plugin artifact `jp.igapyon:miku-docx2md-maven-plugin`
- `convert` and `convert-directory` goals
- plugin parameter documentation
- plugin examples
- Maven plugin smoke verification
- runtime dependency compatibility notes

The separated repository depends on the published or locally installed runtime
artifact `jp.igapyon:miku-docx2md` instead of using a source-tree reactor
module from this repository.

## Reuse Notes

For future similar splits:

1. Create or prepare `<product>-java-maven`.
2. Move Mojo sources, plugin tests, plugin smoke fixtures, examples, and plugin
   docs into the separated repository.
3. Make the separated plugin POM depend on the runtime artifact by normal Maven
   coordinates.
4. Remove the plugin module from `<product>-java`.
5. If the runtime is the only remaining module, move `src/` to the repository
   root and merge the runtime module POM into the root POM.
6. Remove plugin-only properties, plugin management entries, smoke scripts, and
   README usage from the runtime repository.
7. Add cross-links in both repositories so runtime compatibility and ownership
   are clear.
8. Verify the runtime repository with `mvn test` and `mvn package`.
9. Verify the plugin repository after installing or resolving the compatible
   runtime artifact.
