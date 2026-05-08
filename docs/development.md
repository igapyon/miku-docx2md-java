# Development

## Commands

```bash
mvn test
mvn package
java -jar target/miku-docx2md-0.9.0.jar --version
```

Focused tests:

```bash
mvn test -Dtest=MikuDocx2mdCoreTest
mvn test -Dtest=MikuDocx2mdCliTest
```

## Local Workspace

Use `workplace/` for temporary upstream clones, generated comparison output,
and extracted archives. Only `workplace/.gitkeep` is tracked.

## Upstream Maintenance Flow

1. Check the upstream snapshot or current upstream diff.
2. Use `docs/upstream-class-mapping.md` to locate Java classes.
3. Use `docs/upstream-test-mapping.md` to locate focused Java tests.
4. Apply the smallest Java change that preserves upstream behavior.
5. Record the check in `docs/upstream-followup-log.md`.
