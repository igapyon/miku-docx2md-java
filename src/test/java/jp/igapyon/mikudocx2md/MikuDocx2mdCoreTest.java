package jp.igapyon.mikudocx2md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jp.igapyon.mikudocx2md.core.MarkdownOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import org.junit.jupiter.api.Test;

class MikuDocx2mdCoreTest {
    @Test
    void convertsBasicDocumentStructureToMarkdown() {
        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<w:body>"
                + "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:bookmarkStart w:id=\"1\" w:name=\"Section 1\"/><w:r><w:t>Title</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>Hello</w:t></w:r><w:r><w:t xml:space=\"preserve\"> world</w:t></w:r></w:p>"
                + "<w:p><w:r><w:rPr><w:b/><w:i/><w:strike/><w:u/></w:rPr><w:t>Styled</w:t></w:r></w:p>"
                + "<w:p><w:hyperlink r:id=\"rId1\"><w:r><w:t>OpenAI</w:t></w:r></w:hyperlink></w:p>"
                + "<w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Bullet</w:t></w:r></w:p>"
                + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>H1</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>H2</w:t></w:r></w:p></w:tc></w:tr>"
                + "<w:tr><w:tc><w:p><w:r><w:t>A</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>B</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
                + "</w:body></w:document>";
        String relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"https://openai.com\" TargetMode=\"External\"/>"
                + "</Relationships>";
        String numberingXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:numbering xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:abstractNum w:abstractNumId=\"10\"><w:lvl w:ilvl=\"0\"><w:numFmt w:val=\"bullet\"/></w:lvl></w:abstractNum>"
                + "<w:num w:numId=\"1\"><w:abstractNumId w:val=\"10\"/></w:num>"
                + "</w:numbering>";

        ParsedDocx parsed = new MikuDocx2mdCore().parseDocx(DocxFixture.createDocx(documentXml, relsXml, numberingXml, null));
        String markdown = new MikuDocx2mdCore().renderMarkdown(parsed, new MarkdownOptions());

        assertTrue(markdown.contains("<a id=\"section-1\"></a>\n# Title"));
        assertTrue(markdown.contains("Hello world"));
        assertTrue(markdown.contains("***~~<ins>Styled</ins>~~***"));
        assertTrue(markdown.contains("[OpenAI](https://openai.com)"));
        assertTrue(markdown.contains("- Bullet"));
        assertTrue(markdown.contains("| H1 | H2 |"));
        assertEquals(1, parsed.summary.headings);
        assertEquals(3, parsed.summary.paragraphs);
        assertEquals(1, parsed.summary.listItems);
        assertEquals(1, parsed.summary.tables);
        assertEquals(1, parsed.summary.externalLinks);
    }

    @Test
    void rendersImageAssetsAndManifest() {
        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<w:body><w:p><w:r><w:t>Hello CLI</w:t></w:r>"
                + "<w:drawing><wp:inline xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\">"
                + "<wp:docPr id=\"1\" name=\"CLI image\" descr=\"CLI image alt\"/><wp:extent cx=\"635000\" cy=\"317500\"/>"
                + "</wp:inline><a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"><a:graphicData>"
                + "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><pic:blipFill><a:blip r:embed=\"rIdImage1\"/></pic:blipFill></pic:pic>"
                + "</a:graphicData></a:graphic></w:drawing></w:p></w:body></w:document>";
        String relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rIdImage1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/cli-image.png\"/>"
                + "</Relationships>";

        MikuDocx2mdCore core = new MikuDocx2mdCore();
        ParsedDocx parsed = core.parseDocx(DocxFixture.createDocx(documentXml, relsXml, null, DocxFixture.imageFile()));
        MarkdownOptions options = new MarkdownOptions();
        options.imagePathResolver = sourcePath -> "assets/" + sourcePath;
        String markdown = core.renderMarkdown(parsed, options);

        assertTrue(markdown.contains("![CLI image alt](assets/word/media/cli-image.png)"));
        assertEquals(1, parsed.assets.size());
        assertEquals("image/png", parsed.assets.get(0).mediaType);
        assertTrue(core.createAssetsManifestText(parsed).contains("\"sourcePath\": \"word/media/cli-image.png\""));
    }
}
