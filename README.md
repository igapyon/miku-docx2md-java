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
java -jar target/miku-docx2md-0.9.0.jar ./sample.docx --out ./sample.md
```

Summary output:

```bash
java -jar target/miku-docx2md-0.9.0.jar ./sample.docx --out ./sample.md --summary --summary-out ./sample.summary.txt
```

Image assets:

```bash
java -jar target/miku-docx2md-0.9.0.jar ./sample.docx --out ./sample.md --assets-dir ./sample.assets
```

Debug comments:

```bash
java -jar target/miku-docx2md-0.9.0.jar ./sample.docx --out ./sample.md --debug
```

## Current Scope

- Java source / target compatibility: `1.8`
- Build tool: Maven
- Test framework: JUnit Jupiter
- Primary verification: `mvn test`
- Runtime package: executable fat jar
- Distribution package: `target/miku-docx2md-0.9.0-dist.zip`
- Maven plugin: out of initial scope

The initial Java port covers the runtime core and CLI path: document XML,
relationships, styles, numbering, Markdown rendering, image asset extraction,
summary text, and CLI file output. Browser UI behavior from upstream is out of
scope for this Java repository.

## Upstream And Sister Reference

- Upstream Node.js / TypeScript repository: <https://github.com/igapyon/miku-docx2md>
- Local upstream checkout used for this conversion: `../miku-docx2md`
- Upstream snapshot checked locally: branch `tiga0508wfj`, commit `0667817331d617b5c8eec5a529a6b430a4d7b91f`
- Sister Java project: <https://github.com/igapyon/miku-xlsx2md-java>
- Local sister checkout used as the Java shape reference: `../miku-xlsx2md-java`

See `docs/` for upstream class, CLI, and test mapping.

## Repository Operation

`workplace/` is a local scratch area for upstream clones, generated comparison
outputs, extracted archives, and temporary verification artifacts. Only
`workplace/.gitkeep` is tracked.

`.mvn/jvm.config` is tracked for repository-local Maven JVM settings.

## License

Apache License 2.0. See [LICENSE](./LICENSE).
