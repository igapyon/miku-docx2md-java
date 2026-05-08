package jp.igapyon.mikudocx2md.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ZipIo {
    private ZipIo() {
    }

    static Map<String, byte[]> unzipEntries(byte[] bytes) {
        Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
        try {
            ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    files.put(entry.getName(), readAll(zip));
                }
                zip.closeEntry();
            }
            zip.close();
            return files;
        } catch (IOException ex) {
            throw new IllegalArgumentException("ZIP read failed: " + ex.getMessage(), ex);
        }
    }

    private static byte[] readAll(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = zip.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
