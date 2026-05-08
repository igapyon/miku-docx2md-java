package jp.igapyon.mikudocx2md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.igapyon.mikudocx2md.cli.MikuDocx2mdCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuDocx2mdCliTest {
    @TempDir
    Path tempDir;

    @Test
    void printsHelpAndVersion() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int helpStatus = new MikuDocx2mdCli().run(new String[] {"--help"}, new PrintStream(out), new PrintStream(err));
        assertEquals(0, helpStatus);
        assertTrue(out.toString().contains("miku-docx2md - local-first DOCX to Markdown converter"));
        assertTrue(out.toString().contains("manifest.json"));
        assertTrue(out.toString().contains("If --out is omitted, avoid --summary unless mixed stdout output is acceptable."));
        assertTrue(out.toString().contains("Write Markdown to this file. Parent directories are created."));

        out.reset();
        int versionStatus = new MikuDocx2mdCli().run(new String[] {"--version"}, new PrintStream(out), new PrintStream(err));
        assertEquals(0, versionStatus);
        assertEquals("miku-docx2md 0.9.0\n", out.toString());
    }

    @Test
    void rejectsMetadataCommandsMixedWithOtherArguments() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int status = new MikuDocx2mdCli().run(new String[] {"sample.docx", "--version"}, new PrintStream(out), new PrintStream(err));
        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("Use --help or --version without other arguments."));
    }

    @Test
    void printsHelpButFailsWhenInputIsMissing() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(new String[] {}, new PrintStream(out), new PrintStream(err));

        assertEquals(1, status);
        assertTrue(out.toString().contains("USAGE"), out.toString());
        assertEquals("", err.toString());
    }

    @Test
    void rejectsUnknownOptions() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(new String[] {"sample.docx", "--unknown"}, new PrintStream(out), new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("Unknown option: --unknown"), err.toString());
    }

    @Test
    void rejectsMissingOptionValues() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(new String[] {"sample.docx", "--out"}, new PrintStream(out), new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("Missing value for --out"), err.toString());
    }

    @Test
    void rejectsMultipleInputFiles() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(new String[] {"first.docx", "second.docx"}, new PrintStream(out), new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("Specify exactly one input .docx file."), err.toString());
    }

    @Test
    void reportsReadFailuresWithInputDocumentNameAndStage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {"does-not-exist.docx"},
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("[does-not-exist.docx] read failed:"), err.toString());
    }

    @Test
    void writesMarkdownSummaryAndVerboseDiagnostics() throws IOException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        Path markdownOutput = tempDir.resolve("word-bullet-list-basic.md");
        Path summaryOutput = tempDir.resolve("word-bullet-list-basic.summary.txt");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {
                        input.toString(),
                        "--out", markdownOutput.toString(),
                        "--summary-out", summaryOutput.toString(),
                        "--verbose"
                },
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("input=" + input), err.toString());
        assertTrue(err.toString().contains("output=" + markdownOutput), err.toString());
        assertTrue(err.toString().contains("summary=" + summaryOutput), err.toString());
        assertTrue(err.toString().contains("assets=disabled"), err.toString());
        assertTrue(err.toString().contains("input-bytes="), err.toString());
        assertTrue(err.toString().contains("parsed blocks="), err.toString());
        assertTrue(err.toString().contains("summary-written " + summaryOutput), err.toString());
        assertTrue(err.toString().contains("markdown-written " + markdownOutput), err.toString());
        assertTrue(err.toString().contains("done total-ms="), err.toString());
        assertEquals(readTextResource("/expected/markdown/word-bullet-list-basic.md"), readText(markdownOutput));
        assertEquals(readTextResource("/expected/summary/word-bullet-list-basic.txt"), readText(summaryOutput));
    }

    @Test
    void writesMarkdownToStdoutWithoutTrailingNewlineWhenOutIsOmitted() throws IOException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {input.toString()},
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, status);
        assertEquals("", err.toString());
        assertEquals(readTextResource("/expected/markdown/word-bullet-list-basic.md"), out.toString("UTF-8"));
    }

    @Test
    void writesSummaryThenMarkdownToStdoutWhenBothUseStdout() throws IOException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {input.toString(), "--summary"},
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, status);
        assertEquals("", err.toString());
        assertEquals(
                readTextResource("/expected/summary/word-bullet-list-basic.txt")
                        + System.lineSeparator()
                        + readTextResource("/expected/markdown/word-bullet-list-basic.md"),
                out.toString("UTF-8"));
    }

    @Test
    void writesImageAssetsAndManifest() throws IOException {
        Path input = copyResourceToTemp("/docx/word-inline-image-basic.docx", "word-inline-image-basic.docx");
        Path markdownOutput = tempDir.resolve("word-inline-image-basic.md");
        Path assetsDir = tempDir.resolve("assets");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {
                        input.toString(),
                        "--out", markdownOutput.toString(),
                        "--assets-dir", assetsDir.toString()
                },
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(0, status);
        assertEquals("", out.toString());
        assertEquals("", err.toString());
        assertTrue(Files.exists(assetsDir.resolve("word/media/image1.jpeg")));
        String markdown = readText(markdownOutput);
        assertTrue(markdown.contains("![](assets/word/media/image1.jpeg)"), markdown);
        String manifest = readText(assetsDir.resolve("manifest.json"));
        assertTrue(manifest.contains("\"version\": 1"), manifest);
        assertTrue(manifest.contains("\"sourcePath\": \"word/media/image1.jpeg\""), manifest);
        assertTrue(manifest.contains("\"mediaType\": \"image/jpeg\""), manifest);
        assertTrue(manifest.contains("\"size\": 324026"), manifest);
    }

    @Test
    void reportsMarkdownWriteFailuresWithInputDocumentNameAndStage() throws IOException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {
                        input.toString(),
                        "--out", tempDir.toString()
                },
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("[word-bullet-list-basic.docx] markdown write failed:"), err.toString());
    }

    @Test
    void reportsSummaryWriteFailuresWithInputDocumentNameAndStage() throws IOException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {
                        input.toString(),
                        "--summary-out", tempDir.toString()
                },
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("[word-bullet-list-basic.docx] summary write failed:"), err.toString());
    }

    @Test
    void reportsAssetWriteFailuresWithInputDocumentNameAndStage() throws IOException {
        Path input = copyResourceToTemp("/docx/word-inline-image-basic.docx", "word-inline-image-basic.docx");
        Path assetsFile = tempDir.resolve("assets-file");
        Files.write(assetsFile, new byte[] {1});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int status = new MikuDocx2mdCli().run(
                new String[] {
                        input.toString(),
                        "--assets-dir", assetsFile.toString()
                },
                new PrintStream(out),
                new PrintStream(err));

        assertEquals(1, status);
        assertEquals("", out.toString());
        assertTrue(err.toString().contains("[word-inline-image-basic.docx] asset write failed:"), err.toString());
    }

    private Path copyResourceToTemp(String resourceName, String fileName) throws IOException {
        Path output = tempDir.resolve(fileName);
        Files.write(output, readBytesResource(resourceName));
        return output;
    }

    private String readTextResource(String resourceName) throws IOException {
        return new String(readBytesResource(resourceName), StandardCharsets.UTF_8);
    }

    private byte[] readBytesResource(String resourceName) throws IOException {
        InputStream input = MikuDocx2mdCliTest.class.getResourceAsStream(resourceName);
        if (input == null) {
            throw new IOException("Missing test resource: " + resourceName);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private String readText(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
