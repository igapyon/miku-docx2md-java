package jp.igapyon.mikudocx2md.mavenplugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MikuDocx2mdMojoTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsSingleDocx() throws Exception {
        MikuDocx2mdMojo mojo = new MikuDocx2mdMojo();
        Path output = tempDir.resolve("single.md");
        mojo.setInputFile(fixture("word-bullet-list-basic.docx").toFile());
        mojo.setOutputFile(output.toFile());

        mojo.execute();

        assertTrue(Files.readAllLines(output, StandardCharsets.UTF_8).toString().contains("Bullet"));
    }

    @Test
    void convertsDirectory() throws Exception {
        Path inputDirectory = tempDir.resolve("input");
        Files.createDirectories(inputDirectory);
        Files.copy(fixture("word-bullet-list-basic.docx"), inputDirectory.resolve("word-bullet-list-basic.docx"));
        Files.copy(fixture("word-headings-basic.docx"), inputDirectory.resolve("word-headings-basic.docx"));
        Path outputDirectory = tempDir.resolve("output");

        ConvertDirectoryMojo mojo = new ConvertDirectoryMojo();
        mojo.setInputDirectory(inputDirectory.toFile());
        mojo.setOutputDirectory(outputDirectory.toFile());

        mojo.execute();

        assertTrue(Files.exists(outputDirectory.resolve("word-bullet-list-basic.md")));
        assertTrue(Files.exists(outputDirectory.resolve("word-headings-basic.md")));
    }

    private Path fixture(String name) {
        Path direct = Paths.get("..", "miku-docx2md", "src", "test", "resources", "docx", name);
        if (Files.exists(direct)) {
            return direct;
        }
        return Paths.get("miku-docx2md", "src", "test", "resources", "docx", name);
    }
}
