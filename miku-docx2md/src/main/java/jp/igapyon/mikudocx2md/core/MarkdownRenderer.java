package jp.igapyon.mikudocx2md.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import jp.igapyon.mikudocx2md.model.ParsedBlock;
import jp.igapyon.mikudocx2md.model.ParsedDocx;

final class MarkdownRenderer {
    private MarkdownRenderer() {
    }

    static String renderMarkdown(ParsedDocx parsed, MarkdownOptions options) {
        List<RenderedBlock> rendered = new ArrayList<RenderedBlock>();
        for (ParsedBlock block : parsed.blocks) {
            String markdown = renderMarkdownBlock(block, options);
            if (markdown.length() > 0) {
                rendered.add(new RenderedBlock(block.kind, markdown));
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < rendered.size(); index++) {
            if (index > 0) {
                RenderedBlock previous = rendered.get(index - 1);
                RenderedBlock current = rendered.get(index);
                builder.append("listItem".equals(previous.kind) && "listItem".equals(current.kind) ? "\n" : "\n\n");
            }
            builder.append(rendered.get(index).markdown);
        }
        return builder.toString();
    }

    private static String renderMarkdownBlock(ParsedBlock block, MarkdownOptions options) {
        if ("table".equals(block.kind)) {
            return appendUnsupportedArtifacts(renderTable(block.rows), block.unsupportedTypes, options);
        }
        if ("unsupported".equals(block.kind)) {
            String placeholder = renderImagePlaceholder(block.type, options);
            if (options.includeUnsupportedComments) {
                String comment = renderUnsupportedComment(block.type);
                return placeholder.length() > 0 ? placeholder + "\n" + comment : comment;
            }
            return placeholder;
        }
        String content = renderSupportedBlock(block);
        return appendUnsupportedArtifacts(content, block.unsupportedTypes, options);
    }

    private static String renderSupportedBlock(ParsedBlock block) {
        String anchors = renderAnchors(block.anchorIds);
        String line;
        if ("heading".equals(block.kind)) {
            line = repeat("#", Math.max(1, Math.min(block.level, 6))) + " " + block.text;
        } else if ("listItem".equals(block.kind)) {
            line = repeat("    ", Math.max(0, block.indent)) + ("ordered".equals(block.listKind) ? "1." : "-") + " " + block.text;
        } else {
            line = block.text;
        }
        return anchors.length() > 0 ? anchors + "\n" + line : line;
    }

    private static String appendUnsupportedArtifacts(String content, List<String> unsupportedTypes, MarkdownOptions options) {
        String placeholders = renderUnsupportedPlaceholders(unsupportedTypes, options);
        String comments = options.includeUnsupportedComments ? renderUnsupportedComments(unsupportedTypes) : "";
        String result = placeholders.length() > 0 ? content + "\n" + placeholders : content;
        return comments.length() > 0 ? result + "\n" + comments : result;
    }

    private static String renderTable(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendTableRow(builder, rows.get(0));
        builder.append('\n');
        builder.append("| ");
        for (int index = 0; index < rows.get(0).size(); index++) {
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append("---");
        }
        builder.append(" |");
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            builder.append('\n');
            appendTableRow(builder, rows.get(rowIndex));
        }
        return builder.toString();
    }

    private static void appendTableRow(StringBuilder builder, List<String> row) {
        builder.append("| ");
        for (int index = 0; index < row.size(); index++) {
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append(row.get(index).replace("|", "\\|"));
        }
        builder.append(" |");
    }

    private static String renderAnchors(List<String> anchorIds) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < anchorIds.size(); index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append("<a id=\"").append(anchorIds.get(index)).append("\"></a>");
        }
        return builder.toString();
    }

    private static String renderUnsupportedPlaceholders(List<String> unsupportedTypes, MarkdownOptions options) {
        List<String> placeholders = new ArrayList<String>();
        for (String type : unsupportedTypes) {
            String placeholder = renderImagePlaceholder(type, options);
            if (placeholder.length() > 0) {
                placeholders.add(placeholder);
            }
        }
        return join(placeholders, "\n");
    }

    private static String renderImagePlaceholder(String type, MarkdownOptions options) {
        ImageTrace trace = parseImageTrace(type);
        if (trace == null) {
            return "";
        }
        String resolvedPath = options.imagePathResolver == null ? "" : options.imagePathResolver.resolve(trace.sourcePath);
        if (resolvedPath != null && resolvedPath.length() > 0) {
            return "![" + escapeMarkdownImageAltText(trace.altText) + "](" + escapeMarkdownLinkDestination(resolvedPath) + ")";
        }
        return trace.altText.length() == 0 ? "" : "[Image: " + trace.altText.replaceAll("\\s+", " ").trim() + "]";
    }

    private static ImageTrace parseImageTrace(String type) {
        if (type == null || !type.startsWith("drawing:image(")) {
            return null;
        }
        int start = "drawing:image(".length();
        int end = type.indexOf(')', start);
        if (end < 0) {
            return null;
        }
        ImageTrace trace = new ImageTrace();
        trace.sourcePath = type.substring(start, end);
        trace.altText = "";
        String marker = ":alt(";
        int altStart = type.indexOf(marker, end);
        if (altStart >= 0) {
            int altEnd = type.indexOf(')', altStart + marker.length());
            if (altEnd >= 0) {
                trace.altText = type.substring(altStart + marker.length(), altEnd);
            }
        }
        return trace;
    }

    private static String renderUnsupportedComments(List<String> unsupportedTypes) {
        List<String> comments = new ArrayList<String>();
        for (String type : unsupportedTypes) {
            comments.add(renderUnsupportedComment(type));
        }
        return join(comments, "\n");
    }

    private static String renderUnsupportedComment(String type) {
        return "<!-- unsupported: " + (type == null ? "" : type).replace("--", "- -").replace(">", "&gt;") + " -->";
    }

    private static String escapeMarkdownImageAltText(String text) {
        return (text == null ? "" : text).replaceAll("\\s+", " ").replace("[", "").replace("]", "").trim();
    }

    private static String escapeMarkdownLinkDestination(String destination) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < destination.length(); index++) {
            char c = destination.charAt(index);
            if (c == '%') {
                builder.append("%25");
            } else if (Character.isWhitespace(c)) {
                builder.append(urlEncode(Character.toString(c)));
            } else if (c == '(') {
                builder.append("%28");
            } else if (c == ')') {
                builder.append("%29");
            } else if (c == '<') {
                builder.append("%3C");
            } else if (c == '>') {
                builder.append("%3E");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private static String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private static class RenderedBlock {
        String kind;
        String markdown;

        RenderedBlock(String kind, String markdown) {
            this.kind = kind;
            this.markdown = markdown;
        }
    }

    private static class ImageTrace {
        String sourcePath;
        String altText;
    }
}
