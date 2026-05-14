package jp.igapyon.mikudocx2md.core;

import java.nio.file.Path;

public class MikuDocx2mdFileResult {
    public Path inputFile;
    public Path outputFile;
    public Path summaryFile;
    public Path assetsDirectory;
    public String markdown;
    public String summary;
    public int inputBytes;
    public int blockCount;
    public int assetCount;
}
