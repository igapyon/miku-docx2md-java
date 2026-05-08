package jp.igapyon.mikudocx2md;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class DocxFixture {
    private DocxFixture() {
    }

    static byte[] createDocx(String documentXml, String relsXml, String numberingXml, Map<String, byte[]> extraFiles) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes);
            add(zip, "word/document.xml", documentXml.getBytes(StandardCharsets.UTF_8));
            if (relsXml != null) {
                add(zip, "word/_rels/document.xml.rels", relsXml.getBytes(StandardCharsets.UTF_8));
            }
            if (numberingXml != null) {
                add(zip, "word/numbering.xml", numberingXml.getBytes(StandardCharsets.UTF_8));
            }
            add(zip, "[Content_Types].xml", ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"png\" ContentType=\"image/png\"/>"
                    + "</Types>").getBytes(StandardCharsets.UTF_8));
            if (extraFiles != null) {
                for (Map.Entry<String, byte[]> entry : extraFiles.entrySet()) {
                    add(zip, entry.getKey(), entry.getValue());
                }
            }
            zip.close();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static Map<String, byte[]> imageFile() {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        files.put("word/media/cli-image.png", new byte[] {(byte) 137, 80, 78, 71});
        return files;
    }

    private static void add(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }
}
