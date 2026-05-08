package jp.igapyon.mikudocx2md;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import jp.igapyon.mikudocx2md.cli.MikuDocx2mdCli;
import org.junit.jupiter.api.Test;

class MikuDocx2mdCliTest {
    @Test
    void printsHelpAndVersion() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int helpStatus = new MikuDocx2mdCli().run(new String[] {"--help"}, new PrintStream(out), new PrintStream(err));
        assertEquals(0, helpStatus);
        assertTrue(out.toString().contains("miku-docx2md - local-first DOCX to Markdown converter"));
        assertTrue(out.toString().contains("manifest.json"));

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
}
