package jp.igapyon.mikudocx2md.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedDocx {
    public List<ParsedBlock> blocks = new ArrayList<ParsedBlock>();
    public ParsedSummary summary = new ParsedSummary();
    public List<ParsedImageAsset> assets = new ArrayList<ParsedImageAsset>();
    public List<ParsedComment> comments = new ArrayList<ParsedComment>();
}
