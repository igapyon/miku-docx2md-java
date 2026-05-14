# Upstream Class Mapping

| Upstream file | Java class / package | Notes |
| --- | --- | --- |
| `src/ts/core.ts` | `jp.igapyon.mikudocx2md.core.MikuDocx2mdCore` | Public core facade for parse, render, summary, and manifest operations. |
| `src/ts/zip-io.ts` | `jp.igapyon.mikudocx2md.core.ZipIo` | Uses Java `ZipInputStream` instead of manual central-directory parsing. |
| `src/ts/docx-package-loader.ts` | `MikuDocx2mdCore.parseDocx` | Loads `word/document.xml`, relationships, styles, numbering, content types, and package files. |
| `src/ts/xml-utils.ts` | `jp.igapyon.mikudocx2md.xml.XmlUtils` | DOM helper for local-name traversal and attributes. |
| `src/ts/rels-parser.ts` | `MikuDocx2mdCore.parseRelationships` | Preserves target resolution behavior. |
| `src/ts/styles-parser.ts` | `MikuDocx2mdCore.parseStyles`, style helpers | Preserves text style and heading style resolution. |
| `src/ts/numbering-parser.ts` | `MikuDocx2mdCore.parseNumbering`, list helpers | Preserves bullet / ordered list kind resolution. |
| `src/ts/document-parser.ts` | `MikuDocx2mdCore.parseDocumentXml` | Java entrypoint for document XML conversion. |
| `src/ts/document-block-parser.ts` | `MikuDocx2mdCore.parseBody`, `parseParagraph`, `parseTable` | Preserves block dispatch and summary counters. |
| `src/ts/document-inline-parser.ts` | `MikuDocx2mdCore.extractTextRuns`, `renderRun`, `extractTextboxText` | Preserves runs, line breaks, hyperlinks, textboxes, and unsupported traces. |
| `src/ts/document-paragraph-parser.ts` | `MikuDocx2mdCore.getHeadingLevel`, `getListMetadata`, `renderStructuredParagraphText` | Preserves heading/list rendering for body and table cells. |
| `src/ts/document-table-parser.ts` | `MikuDocx2mdCore.parseTable` | Preserves grid span and vertical merge placeholders. |
| `src/ts/document-cell-parser.ts` | `MikuDocx2mdCore.extractCellText` | Preserves `<br><br>` joining of cell paragraphs. |
| `src/ts/document-text-style-parser.ts` | `MikuDocx2mdCore` text style helpers | Preserves bold, italic, strike, and underline order. |
| `src/ts/document-hyperlink-parser.ts` | `MikuDocx2mdCore.renderHyperlink` | Preserves external and known internal link behavior. |
| `src/ts/document-drawing-parser.ts` | `MikuDocx2mdCore.describeUnsupported` and image trace helpers | Preserves drawing image trace format. |
| `src/ts/docx-assets.ts` | `MikuDocx2mdCore.collectImageAssets` | Preserves safe image asset extraction and media type resolution. |
| `src/ts/markdown-renderer.ts` | `jp.igapyon.mikudocx2md.core.MarkdownRenderer` | Preserves Markdown block rendering and unsupported image placeholders. |
| `src/ts/summary.ts`, `src/ts/document-summary.ts` | `jp.igapyon.mikudocx2md.model.ParsedSummary` | Preserves summary field names and text order. |
| `src/ts/asset-manifest.ts` | `jp.igapyon.mikudocx2md.core.AssetManifest` | Preserves image manifest contract. |
| `scripts/miku-docx2md-cli.mjs` | `jp.igapyon.mikudocx2md.cli.MikuDocx2mdCli`, `CliOptions` | Preserves CLI options, stdout/stderr roles, and exit codes. |

Browser-only files such as `src/ts/main.ts`, `browser-zip.ts`, and
`browser-assets-export.ts` are out of scope for the Java runtime.
