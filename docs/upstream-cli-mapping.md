# Upstream CLI Mapping

| Upstream Node CLI | Java CLI |
| --- | --- |
| `node scripts/miku-docx2md-cli.mjs <input.docx>` | `java -jar miku-docx2md/target/miku-docx2md-0.9.0.jar <input.docx>` |
| `--out <file>` | `--out <file>` |
| `--assets-dir <dir>` | `--assets-dir <dir>` |
| `--summary` | `--summary` |
| `--summary-out <file>` | `--summary-out <file>` |
| `--debug` | `--debug` |
| `--include-unsupported-comments` | `--include-unsupported-comments` |
| `--verbose` | `--verbose` |
| `--version` | `--version` |
| `--help` | `--help` |

## Java-Side Extensions

These options intentionally follow the closest sister project
`workplace/miku-xlsx2md-java-devel` and extend beyond the upstream Node CLI.

| Java CLI extension | Purpose |
| --- | --- |
| multiple positional `.docx` files | Convert several input files in one command. |
| `--input-directory <dir>` | Convert `.docx` files under a directory. |
| `--output-directory <dir>` | Write batch Markdown files under a directory. |
| `--recursive` | Recursively scan `--input-directory`. |

## Stdout And Stderr

- Markdown goes to stdout when `--out` is omitted.
- Markdown stdout uses the upstream raw-write behavior and does not add an
  extra trailing newline.
- Markdown is written to the specified file when `--out` is present.
- `--summary` writes summary text to stdout.
- If `--summary` is used without `--out`, summary text is printed first,
  followed by Markdown on the same stdout stream.
- `--summary-out` writes summary text to a file.
- `--verbose` writes diagnostics to stderr with the `verbose:` prefix.
- Verbose diagnostics include input, output, summary, assets, byte count,
  parsed block / asset counts, written outputs, and total elapsed time.
- `--help` returns exit code `0`.
- Missing input prints help and returns exit code `1`.
- Usage and runtime failures return exit code `1`.
- Read failures use the `read failed` stage.
- Output failures use `markdown write failed`, `summary write failed`, or
  `asset write failed` according to the output path.

## Known Runtime Differences

- Java uses `ZipInputStream` for DOCX package loading, while upstream Node has a
  small ZIP parser that can also run in browser environments.
- Browser UI and browser-only export paths are out of scope.
- Java dependency footprint is currently standard-library only for runtime.
