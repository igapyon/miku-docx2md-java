package jp.igapyon.mikudocx2md.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParsedSummary {
    public int paragraphs;
    public int headings;
    public int listItems;
    public int tables;
    public int images;
    public int imageAssets;
    public int drawingLikeUnsupported;
    public int links;
    public int internalLinks;
    public int externalLinks;
    public int unsupportedElements;
    public int unsupportedCommentTraces;

    public void recordUnsupported(String type) {
        if (type != null && type.startsWith("drawing")) {
            drawingLikeUnsupported++;
        }
        if (type != null && type.startsWith("drawing:image(")) {
            images++;
        }
        unsupportedElements++;
        unsupportedCommentTraces++;
    }

    public Map<String, Integer> asMap() {
        Map<String, Integer> fields = new LinkedHashMap<String, Integer>();
        fields.put("paragraphs", paragraphs);
        fields.put("headings", headings);
        fields.put("listItems", listItems);
        fields.put("tables", tables);
        fields.put("images", images);
        fields.put("imageAssets", imageAssets);
        fields.put("drawingLikeUnsupported", drawingLikeUnsupported);
        fields.put("links", links);
        fields.put("internalLinks", internalLinks);
        fields.put("externalLinks", externalLinks);
        fields.put("unsupportedElements", unsupportedElements);
        fields.put("unsupportedCommentTraces", unsupportedCommentTraces);
        return fields;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : asMap().entrySet()) {
            if (!first) {
                builder.append('\n');
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }
}
