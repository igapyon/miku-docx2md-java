package jp.igapyon.mikudocx2md.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedBlock {
    public String kind;
    public String text;
    public int level;
    public String listKind;
    public int indent;
    public String type;
    public List<String> anchorIds = new ArrayList<String>();
    public List<String> unsupportedTypes = new ArrayList<String>();
    public List<List<String>> rows = new ArrayList<List<String>>();

    public static ParsedBlock paragraph(String kind, String text) {
        ParsedBlock block = new ParsedBlock();
        block.kind = kind;
        block.text = text;
        return block;
    }

    public static ParsedBlock unsupported(String type) {
        ParsedBlock block = new ParsedBlock();
        block.kind = "unsupported";
        block.type = type;
        return block;
    }

    public static ParsedBlock table(List<List<String>> rows) {
        ParsedBlock block = new ParsedBlock();
        block.kind = "table";
        block.rows = rows;
        return block;
    }
}
