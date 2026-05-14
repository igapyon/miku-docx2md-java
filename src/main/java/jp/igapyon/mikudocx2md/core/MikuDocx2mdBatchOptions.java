package jp.igapyon.mikudocx2md.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MikuDocx2mdBatchOptions {
    public final List<Path> inputFiles = new ArrayList<Path>();
    public Path inputDirectory;
    public Path outputDirectory;
    public Path assetsDirectory;
    public boolean recursive;
    public boolean includeUnsupportedComments;
    public MikuDocx2mdConversionListener listener;
}
