# Upstream Test Mapping

| Upstream test intent | Java test | Focused command |
| --- | --- | --- |
| CLI help and version metadata work without an input file. | `MikuDocx2mdCliTest.printsHelpAndVersion` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Metadata commands cannot be mixed with other arguments. | `MikuDocx2mdCliTest.rejectsMetadataCommandsMixedWithOtherArguments` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI metadata stdout and representative usage errors stay aligned with upstream generated output. | `scripts/compare-node-java-cli.sh` | `scripts/compare-node-java-cli.sh` |
| Missing input prints help but returns exit code `1`. | `MikuDocx2mdCliTest.printsHelpButFailsWhenInputIsMissing` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Unknown options are rejected with the upstream usage error text. | `MikuDocx2mdCliTest.rejectsUnknownOptions` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Value options without a value are rejected with the upstream usage error text. | `MikuDocx2mdCliTest.rejectsMissingOptionValues` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Java-side batch CLI converts multiple positional input files to an output directory. | `MikuDocx2mdCliTest.writesMultipleInputFilesToOutputDirectory` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Java-side batch CLI converts `--input-directory` to `--output-directory`. | `MikuDocx2mdCliTest.writesInputDirectoryToOutputDirectory` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI read failures include the input document name and `read failed` stage. | `MikuDocx2mdCliTest.reportsReadFailuresWithInputDocumentNameAndStage` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI markdown write failures include the input document name and `markdown write failed` stage. | `MikuDocx2mdCliTest.reportsMarkdownWriteFailuresWithInputDocumentNameAndStage` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI summary write failures include the input document name and `summary write failed` stage. | `MikuDocx2mdCliTest.reportsSummaryWriteFailuresWithInputDocumentNameAndStage` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI asset write failures include the input document name and `asset write failed` stage. | `MikuDocx2mdCliTest.reportsAssetWriteFailuresWithInputDocumentNameAndStage` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI writes Markdown and summary files and keeps upstream-aligned verbose diagnostics on stderr. | `MikuDocx2mdCliTest.writesMarkdownSummaryAndVerboseDiagnostics` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI `--verbose` diagnostics stay aligned with upstream after normalizing elapsed times and comparison work directories. | `scripts/compare-node-java-cli.sh` | `scripts/compare-node-java-cli.sh` |
| CLI writes Markdown to stdout without adding an upstream-extra trailing newline. | `MikuDocx2mdCliTest.writesMarkdownToStdoutWithoutTrailingNewlineWhenOutIsOmitted` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI writes summary first and Markdown second when both are routed to stdout. | `MikuDocx2mdCliTest.writesSummaryThenMarkdownToStdoutWhenBothUseStdout` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| CLI `--debug` and `--include-unsupported-comments` generate upstream-aligned unsupported trace comments. | `scripts/compare-node-java-cli.sh` | `scripts/compare-node-java-cli.sh` |
| CLI writes resolved image assets and `manifest.json` under `--assets-dir`. | `MikuDocx2mdCliTest.writesImageAssetsAndManifest` | `mvn test -Dtest=MikuDocx2mdCliTest` |
| Basic document structure converts to Markdown. | `MikuDocx2mdCoreTest.convertsBasicDocumentStructureToMarkdown` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Relationships, hyperlinks, numbering, tables, and style markers are preserved. | `MikuDocx2mdCoreTest.convertsBasicDocumentStructureToMarkdown` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Drawing image traces become Markdown image placeholders and extracted assets. | `MikuDocx2mdCoreTest.rendersImageAssetsAndManifest` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Asset manifest includes image source path, media type, trace, and document position. | `MikuDocx2mdCoreTest.rendersImageAssetsAndManifest` | `mvn test -Dtest=MikuDocx2mdCoreTest` |
| Upstream checked-in `.docx` fixtures render the expected Markdown captured from the Node CLI. | `MikuDocx2mdFixtureParityTest.rendersExpectedMarkdownForUpstreamFixtures` | `mvn test -Dtest=MikuDocx2mdFixtureParityTest` |
| Upstream fixture summary text matches the expected summary captured from the Node runtime. | `MikuDocx2mdFixtureParityTest.producesStableSummaryTextForUpstreamFixtures` | `mvn test -Dtest=MikuDocx2mdFixtureParityTest` |
| Upstream image fixtures produce one resolved image asset. | `MikuDocx2mdFixtureParityTest.imageFixtureAssetCountsStayAligned` | `mvn test -Dtest=MikuDocx2mdFixtureParityTest` |
| Java-side Maven plugin converts a single DOCX file. | `MikuDocx2mdMojoTest.convertsSingleDocx` | `mvn -pl miku-docx2md-maven-plugin -am -Dtest=MikuDocx2mdMojoTest test` |
| Java-side Maven plugin converts a DOCX directory. | `MikuDocx2mdMojoTest.convertsDirectory` | `mvn -pl miku-docx2md-maven-plugin -am -Dtest=MikuDocx2mdMojoTest test` |

## Follow-up

The upstream fixture corpus has been copied into
`miku-docx2md/src/test/resources/docx/`.
Expected Markdown captured from the Node CLI is tracked under
`miku-docx2md/src/test/resources/expected/markdown/`. Expected summary text
captured from the Node runtime is tracked under
`miku-docx2md/src/test/resources/expected/summary/`. Use
`scripts/compare-node-java-cli.sh` for upstream CLI metadata, usage errors,
verbose diagnostics, generated Markdown, summary, stdout Markdown, mixed
summary/Markdown stdout, debug Markdown, `--include-unsupported-comments`
Markdown, manifest, and asset comparisons against a local upstream checkout.
