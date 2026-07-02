package jp.igapyon.mikudocx2md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdBatchOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdBatchResult;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdConversionException;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileConverter;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuDocx2mdFileConverterTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsSingleFileThroughRuntimeFileApi() throws IOException, MikuDocx2mdConversionException {
        Path input = copyResourceToTemp("/docx/word-bullet-list-basic.docx", "word-bullet-list-basic.docx");
        Path markdownOutput = tempDir.resolve("word-bullet-list-basic.md");
        Path summaryOutput = tempDir.resolve("word-bullet-list-basic.summary.txt");
        MikuDocx2mdFileOptions options = new MikuDocx2mdFileOptions();
        options.inputFile = input;
        options.outputFile = markdownOutput;
        options.summaryFile = summaryOutput;

        MikuDocx2mdFileResult result = new MikuDocx2mdFileConverter().convertFile(options);

        assertEquals(markdownOutput, result.outputFile);
        assertEquals(summaryOutput, result.summaryFile);
        assertEquals(withFrontMatter("word-bullet-list-basic.docx", readTextResource("/expected/markdown/word-bullet-list-basic.md")), readText(markdownOutput));
        assertEquals(readTextResource("/expected/summary/word-bullet-list-basic.txt"), readText(summaryOutput));
        assertEquals(readText(markdownOutput), result.markdown);
        assertEquals(readText(summaryOutput), result.summary);
    }

    @Test
    void convertsInputDirectoryThroughRuntimeBatchApi() throws IOException, MikuDocx2mdConversionException {
        Path inputDirectory = tempDir.resolve("input-dir");
        Files.createDirectories(inputDirectory.resolve("nested"));
        Files.write(inputDirectory.resolve("word-bullet-list-basic.docx"), readBytesResource("/docx/word-bullet-list-basic.docx"));
        Files.write(inputDirectory.resolve("nested").resolve("word-headings-basic.docx"), readBytesResource("/docx/word-headings-basic.docx"));
        Path outputDirectory = tempDir.resolve("output-dir");
        MikuDocx2mdBatchOptions options = new MikuDocx2mdBatchOptions();
        options.inputDirectory = inputDirectory;
        options.outputDirectory = outputDirectory;
        options.recursive = true;

        MikuDocx2mdBatchResult result = new MikuDocx2mdFileConverter().convertBatch(options);

        assertEquals(2, result.getConvertedCount());
        assertTrue(Files.exists(outputDirectory.resolve("word-bullet-list-basic.md")));
        assertTrue(Files.exists(outputDirectory.resolve("nested").resolve("word-headings-basic.md")));
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
        InputStream input = MikuDocx2mdFileConverterTest.class.getResourceAsStream(resourceName);
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

    private String withFrontMatter(String title, String body) {
        return "---\n"
                + "title: \"" + title + "\"\n"
                + "type: converted\n"
                + "conversion:\n"
                + "  tool: miku-docx2md\n"
                + "  version: \"1.2.1\"\n"
                + "  unsupported_comments: exclude\n"
                + "---\n\n"
                + body;
    }
}
