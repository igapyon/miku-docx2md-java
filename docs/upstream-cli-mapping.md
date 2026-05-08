# Upstream CLI Mapping

| Upstream Node CLI | Java CLI |
| --- | --- |
| `node scripts/miku-docx2md-cli.mjs <input.docx>` | `java -jar target/miku-docx2md-0.9.0.jar <input.docx>` |
| `--out <file>` | `--out <file>` |
| `--assets-dir <dir>` | `--assets-dir <dir>` |
| `--summary` | `--summary` |
| `--summary-out <file>` | `--summary-out <file>` |
| `--debug` | `--debug` |
| `--include-unsupported-comments` | `--include-unsupported-comments` |
| `--verbose` | `--verbose` |
| `--version` | `--version` |
| `--help` | `--help` |

## Stdout And Stderr

- Markdown goes to stdout when `--out` is omitted.
- Markdown is written to the specified file when `--out` is present.
- `--summary` writes summary text to stdout.
- `--summary-out` writes summary text to a file.
- `--verbose` writes diagnostics to stderr with the `verbose:` prefix.
- Usage and runtime failures return exit code `1`.

## Known Runtime Differences

- Java uses `ZipInputStream` for DOCX package loading, while upstream Node has a
  small ZIP parser that can also run in browser environments.
- Browser UI and browser-only export paths are out of scope.
- Java dependency footprint is currently standard-library only for runtime.
