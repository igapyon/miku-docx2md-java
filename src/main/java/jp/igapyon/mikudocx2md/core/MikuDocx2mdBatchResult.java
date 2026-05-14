package jp.igapyon.mikudocx2md.core;

import java.util.ArrayList;
import java.util.List;

public class MikuDocx2mdBatchResult {
    public final List<MikuDocx2mdFileResult> files = new ArrayList<MikuDocx2mdFileResult>();

    public int getConvertedCount() {
        return files.size();
    }
}
