# Upstream Test Mapping

| Upstream test intent | Java test | Focused command |
| --- | --- | --- |
| CLI help and version metadata work without an input file. | `MikuDocx2mdCliTest.printsHelpAndVersion` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Metadata commands cannot be mixed with other arguments. | `MikuDocx2mdCliTest.rejectsMetadataCommandsMixedWithOtherArguments` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Basic document structure converts to Markdown. | `MikuDocx2mdCoreTest.convertsBasicDocumentStructureToMarkdown` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Relationships, hyperlinks, numbering, tables, and style markers are preserved. | `MikuDocx2mdCoreTest.convertsBasicDocumentStructureToMarkdown` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Drawing image traces become Markdown image placeholders and extracted assets. | `MikuDocx2mdCoreTest.rendersImageAssetsAndManifest` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Asset manifest includes image source path, media type, trace, and document position. | `MikuDocx2mdCoreTest.rendersImageAssetsAndManifest` | `mvn test -Dtest=MikuDocx2mdCoreTest` |

## Follow-up

The upstream fixture corpus under `../miku-docx2md/tests/fixtures/docx/` should
be added to Java parity coverage after the initial runtime shape is stable.
