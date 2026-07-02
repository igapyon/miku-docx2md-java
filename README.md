# miku-docx2md-java

`miku-docx2md-java` is the Java straight-conversion runtime and CLI for
[`miku-docx2md`](https://github.com/igapyon/miku-docx2md).

The tool converts local `.docx` files to Markdown. The Java version keeps the
Node.js / TypeScript upstream vocabulary and observable CLI behavior traceable,
while packaging the converter as a Maven-built executable jar.

## Usage

Build:

```bash
mvn test
mvn package
```

Run:

```bash
java -jar target/miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md
```

Summary output:

```bash
java -jar target/miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --summary --summary-out ./sample.summary.txt
```

Image assets:

```bash
java -jar target/miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --assets-dir ./sample.assets
```

Debug comments:

```bash
java -jar target/miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --debug
```

Omit YAML front matter:

```bash
java -jar target/miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --front-matter exclude
```

Batch directory conversion:

```bash
java -jar target/miku-docx2md-1.0.0.jar \
  --input-directory ./docx \
  --output-directory ./markdown \
  --recursive
```

## Current Scope

- Java source / target compatibility: `1.8`
- Build tool: Maven
- Test framework: JUnit Jupiter
- Primary verification: `mvn test`
- Runtime package: executable fat jar under `target/`
- Distribution package: `target/miku-docx2md-1.0.0-dist.zip`

The Java port covers the runtime core, CLI path, document XML, relationships,
styles, numbering, Word comments as Markdown footnotes, tracked insertion /
deletion markup, Markdown rendering, YAML front matter, image asset extraction,
summary text, file output, and Java-side batch conversion. Browser UI behavior
from upstream is out of scope for this Java repository.

Maven plugin support is separated into
[`miku-docx2md-java-maven`](https://github.com/igapyon/miku-docx2md-java-maven).
That repository owns `jp.igapyon:miku-docx2md-maven-plugin` and its Maven goal
documentation, examples, and smoke tests.

GitHub Release asset workflow support is provided by
`.github/workflows/release-cli-runtime.yml`. It builds from `v*` tags or manual
`tag_name` dispatch and uploads the runtime jar plus sources jar.

## Upstream And Sister Reference

- Upstream Node.js / TypeScript repository: <https://github.com/igapyon/miku-docx2md>
- Local upstream checkout used for this conversion: `../miku-docx2md`
- Upstream snapshot checked locally: branch `tiga0508wfj`, commit `0667817331d617b5c8eec5a529a6b430a4d7b91f`
- Sister Java project: <https://github.com/igapyon/miku-xlsx2md-java>
- Local sister checkout used as the Java shape reference: `../miku-xlsx2md-java`
- Closest local sister checkout used for historical Maven plugin and batch CLI
  shape: `workplace/miku-xlsx2md-java-devel`

See `docs/` for upstream class, CLI, and test mapping.

## Repository Operation

`workplace/` is a local scratch area for upstream clones, generated comparison
outputs, extracted archives, and temporary verification artifacts. Only
`workplace/.gitkeep` is tracked.

`.mvn/jvm.config` is tracked for repository-local Maven JVM settings.

## License

Apache License 2.0. See [LICENSE](./LICENSE).
