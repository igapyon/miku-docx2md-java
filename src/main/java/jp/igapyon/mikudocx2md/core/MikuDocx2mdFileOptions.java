package jp.igapyon.mikudocx2md.core;

import java.nio.file.Path;

public class MikuDocx2mdFileOptions {
    public Path inputFile;
    public Path outputFile;
    public Path summaryFile;
    public Path assetsDirectory;
    public boolean summaryToStdout;
    public boolean includeUnsupportedComments;
    public String frontMatter = "include";
    public MikuDocx2mdConversionListener listener;
}
