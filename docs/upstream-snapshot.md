# Upstream Snapshot

## Source

- Upstream repository: <https://github.com/igapyon/miku-docx2md>
- Local upstream checkout: `../miku-docx2md`
- Snapshot branch checked during initial conversion: `tiga0508wfj`
- Snapshot commit checked during initial conversion: `0667817331d617b5c8eec5a529a6b430a4d7b91f`
- Latest upstream follow-up checked locally: branch `devel-tiga0702vfd`,
  tag `v1.2.1`, commit `f5bdf21`
- Upstream reference method: local checkout, not vendored

## Sister Reference

- Sister Java repository: <https://github.com/igapyon/miku-xlsx2md-java>
- Local sister checkout used initially: `../miku-xlsx2md-java`
- Closest sister checkout used for Maven plugin and batch CLI follow-up:
  `workplace/miku-xlsx2md-java-devel`

The sister project influenced these initial repository-shape decisions:

- Maven as the build tool
- Java 8 source and target compatibility
- JUnit Jupiter tests
- thin CLI entrypoint delegating to core runtime
- initial multi-module runtime / Maven plugin shape, later separated into
  `miku-docx2md-java` and `miku-docx2md-java-maven`
- executable shaded runtime jar
- `docs/` mapping and status documents
- `workplace/.gitkeep` as the only tracked workplace file

The closest sister project's Maven plugin and directory conversion shape was
used for the Java-side follow-up that added `miku-docx2md-maven-plugin` and
batch CLI conversion. The Maven plugin adapter was later separated to
<https://github.com/igapyon/miku-docx2md-java-maven>.
