package jp.igapyon.mikudocx2md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import jp.igapyon.mikudocx2md.core.MarkdownOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MikuDocx2mdFixtureParityTest {
    private static final MikuDocx2mdCore CORE = new MikuDocx2mdCore();

    static Collection<String> upstreamFixtures() {
        return Arrays.asList(
                "word-bullet-list-basic.docx",
                "word-headings-basic.docx",
                "word-image-alt-text-basic.docx",
                "word-inline-formatting-basic.docx",
                "word-inline-image-basic.docx",
                "word-links-basic.docx",
                "word-nested-list-basic.docx",
                "word-numbered-list-basic.docx",
                "word-table-merged-cell-basic.docx");
    }

    @ParameterizedTest
    @MethodSource("upstreamFixtures")
    void rendersExpectedMarkdownForUpstreamFixtures(String fixtureName) throws IOException {
        ParsedDocx parsed = CORE.parseDocx(readFixture(fixtureName));
        String markdown = CORE.renderMarkdown(parsed, new MarkdownOptions());
        String expectedMarkdown = readTextResource("/expected/markdown/" + fixtureName.replace(".docx", ".md"));

        assertFalse(parsed.blocks.isEmpty(), fixtureName);
        assertEquals(expectedMarkdown, markdown, fixtureName);
    }

    @ParameterizedTest
    @MethodSource("upstreamFixtures")
    void producesStableSummaryTextForUpstreamFixtures(String fixtureName) throws IOException {
        ParsedDocx parsed = CORE.parseDocx(readFixture(fixtureName));
        String summary = CORE.createSummaryText(parsed);
        String expectedSummary = readTextResource("/expected/summary/" + fixtureName.replace(".docx", ".txt"));

        assertEquals(expectedSummary, summary, fixtureName);
    }

    @ParameterizedTest
    @MethodSource("upstreamFixtures")
    void imageFixtureAssetCountsStayAligned(String fixtureName) throws IOException {
        ParsedDocx parsed = CORE.parseDocx(readFixture(fixtureName));

        if ("word-inline-image-basic.docx".equals(fixtureName) || "word-image-alt-text-basic.docx".equals(fixtureName)) {
            assertEquals(1, parsed.assets.size(), fixtureName);
            assertEquals(1, parsed.summary.imageAssets, fixtureName);
        }
    }

    private byte[] readFixture(String fixtureName) throws IOException {
        return readBytesResource("/docx/" + fixtureName);
    }

    private String readTextResource(String resourceName) throws IOException {
        return new String(readBytesResource(resourceName), StandardCharsets.UTF_8);
    }

    private byte[] readBytesResource(String resourceName) throws IOException {
        InputStream input = MikuDocx2mdFixtureParityTest.class.getResourceAsStream(resourceName);
        if (input == null) {
            throw new IOException("Missing test fixture: " + resourceName);
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
}
