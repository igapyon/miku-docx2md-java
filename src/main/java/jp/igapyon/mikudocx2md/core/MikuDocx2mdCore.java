package jp.igapyon.mikudocx2md.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jp.igapyon.mikudocx2md.model.ParsedBlock;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import jp.igapyon.mikudocx2md.model.ParsedImageAsset;
import jp.igapyon.mikudocx2md.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MikuDocx2mdCore {
    public static final String VERSION = "0.9.0";

    public ParsedDocx parseDocx(byte[] bytes) {
        Map<String, byte[]> files = ZipIo.unzipEntries(bytes);
        byte[] documentXml = files.get("word/document.xml");
        if (documentXml == null) {
            throw new IllegalArgumentException("word/document.xml was not found.");
        }

        ParsedDocx parsed = parseDocumentXml(
                documentXml,
                files.get("word/_rels/document.xml.rels"),
                files.get("word/styles.xml"),
                files.get("word/numbering.xml"));
        collectImageAssets(parsed, files, files.get("[Content_Types].xml"));
        parsed.summary.imageAssets = parsed.assets.size();
        return parsed;
    }

    public ParsedDocx parseDocumentXml(byte[] documentXml, byte[] relationshipsXml, byte[] stylesXml, byte[] numberingXml) {
        Document document = XmlUtils.parseXml(documentXml);
        Element body = firstDescendant(document, "body");
        Map<String, Relationship> relationships = relationshipsXml == null
                ? new LinkedHashMap<String, Relationship>()
                : parseRelationships(relationshipsXml, "word/document.xml");
        Map<String, StyleDefinition> styles = parseStyles(stylesXml);
        Numbering numbering = parseNumbering(numberingXml);
        return parseBody(body, relationships, styles, numbering);
    }

    public String renderMarkdown(ParsedDocx parsed, MarkdownOptions options) {
        return MarkdownRenderer.renderMarkdown(parsed, options == null ? new MarkdownOptions() : options);
    }

    public String createSummaryText(ParsedDocx parsed) {
        return parsed.summary.toText();
    }

    public String createAssetsManifestText(ParsedDocx parsed) {
        return AssetManifest.createAssetsManifestText(parsed.assets);
    }

    private ParsedDocx parseBody(Element body, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles, Numbering numbering) {
        ParsedDocx parsed = new ParsedDocx();
        if (body == null) {
            return parsed;
        }
        Set<String> knownAnchorIds = collectKnownAnchorIds(body);
        Set<String> emittedAnchorIds = new HashSet<String>();
        NodeList children = body.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            String local = XmlUtils.localName(element);
            ParsedBlock block = null;
            if ("p".equals(local)) {
                block = parseParagraph(element, relationships, styles, numbering, parsed, knownAnchorIds, emittedAnchorIds);
            } else if ("tbl".equals(local)) {
                parsed.summary.tables++;
                block = parseTable(element, relationships, styles, numbering, parsed, knownAnchorIds);
            } else {
                String type = describeUnsupported(element, relationships);
                parsed.summary.recordUnsupported(type);
                block = ParsedBlock.unsupported(type);
            }
            if (block != null) {
                parsed.blocks.add(block);
            }
        }
        return parsed;
    }

    private ParsedBlock parseParagraph(Element paragraph, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            Numbering numbering, ParsedDocx parsed, Set<String> knownAnchorIds, Set<String> emittedAnchorIds) {
        List<String> unsupported = new ArrayList<String>();
        String text = extractTextRuns(paragraph, relationships, styles, numbering, parsed, knownAnchorIds, unsupported, getParagraphTextStyle(paragraph, styles), false);
        if (text.length() == 0) {
            return unsupported.isEmpty() ? null : ParsedBlock.unsupported(unsupported.get(0));
        }
        Integer headingLevel = getHeadingLevel(paragraph, styles);
        ListMetadata list = getListMetadata(paragraph, numbering);
        ParsedBlock block;
        if (list != null) {
            parsed.summary.listItems++;
            block = ParsedBlock.paragraph("listItem", text);
            block.listKind = list.kind;
            block.indent = list.indent;
        } else if (headingLevel != null) {
            parsed.summary.headings++;
            block = ParsedBlock.paragraph("heading", text);
            block.level = headingLevel.intValue();
        } else {
            parsed.summary.paragraphs++;
            block = ParsedBlock.paragraph("paragraph", text);
        }
        block.anchorIds.addAll(claimUniqueAnchors(extractParagraphAnchors(paragraph), emittedAnchorIds));
        block.unsupportedTypes.addAll(unsupported);
        return block;
    }

    private ParsedBlock parseTable(Element table, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            Numbering numbering, ParsedDocx parsed, Set<String> knownAnchorIds) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> unsupported = new ArrayList<String>();
        for (Element rowElement : XmlUtils.childrenByLocalName(table, "tr")) {
            List<String> row = new ArrayList<String>();
            for (Element cellElement : XmlUtils.childrenByLocalName(rowElement, "tc")) {
                int span = getGridSpan(cellElement);
                String verticalMerge = getVerticalMergeState(cellElement);
                String text = extractCellText(cellElement, relationships, styles, numbering, parsed, knownAnchorIds, unsupported);
                if ("continue".equals(verticalMerge)) {
                    for (int index = 0; index < span; index++) {
                        row.add(index == 0 ? "↑M↑" : "←M←");
                    }
                    continue;
                }
                row.add(text);
                for (int index = 1; index < span; index++) {
                    row.add("←M←");
                }
            }
            rows.add(row);
        }
        normalizeRows(rows);
        ParsedBlock block = ParsedBlock.table(rows);
        block.unsupportedTypes.addAll(unsupported);
        return block;
    }

    private String extractCellText(Element cell, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            Numbering numbering, ParsedDocx parsed, Set<String> knownAnchorIds, List<String> unsupported) {
        List<String> parts = new ArrayList<String>();
        for (Element paragraph : XmlUtils.childrenByLocalName(cell, "p")) {
            String text = extractTextRuns(paragraph, relationships, styles, numbering, parsed, knownAnchorIds, unsupported, getParagraphTextStyle(paragraph, styles), false);
            if (text.length() > 0) {
                parts.add(renderStructuredParagraphText(paragraph, text, styles, numbering));
            }
        }
        return join(parts, "<br><br>").trim();
    }

    private String extractTextRuns(Element parent, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            Numbering numbering, ParsedDocx parsed, Set<String> knownAnchorIds, List<String> unsupported, TextStyle inheritedStyle,
            boolean suppressUnderline) {
        List<String> pieces = new ArrayList<String>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            String local = XmlUtils.localName(element);
            if ("r".equals(local)) {
                pieces.add(renderRun(element, relationships, styles, parsed, unsupported, inheritedStyle, suppressUnderline));
            } else if ("hyperlink".equals(local)) {
                String linkText = extractTextRuns(element, relationships, styles, numbering, parsed, knownAnchorIds, unsupported, inheritedStyle, true);
                pieces.add(renderHyperlink(element, linkText, relationships, parsed, knownAnchorIds));
            } else if ("txbxContent".equals(local)) {
                String textbox = extractTextboxText(element, relationships, styles, numbering, parsed, knownAnchorIds, unsupported);
                if (textbox.length() > 0) {
                    if (!pieces.isEmpty()) {
                        pieces.add("<br><br>");
                    }
                    pieces.add(textbox);
                }
            } else if ("bookmarkStart".equals(local) || "bookmarkEnd".equals(local) || "pPr".equals(local) || "proofErr".equals(local)) {
                continue;
            } else {
                recordUnsupportedTrace(parsed, unsupported, describeUnsupported(element, relationships));
            }
        }
        return normalizeInlineText(join(pieces, ""));
    }

    private String renderRun(Element run, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            ParsedDocx parsed, List<String> unsupported, TextStyle inheritedStyle, boolean suppressUnderline) {
        TextStyle style = resolveRunTextStyle(run, styles, inheritedStyle, suppressUnderline);
        List<String> pieces = new ArrayList<String>();
        NodeList children = run.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            String local = XmlUtils.localName(element);
            if ("t".equals(local)) {
                pieces.add(applyTextStyle(element.getTextContent(), style));
            } else if ("br".equals(local)) {
                pieces.add("<br>");
            } else if ("drawing".equals(local) || "pict".equals(local) || "object".equals(local)) {
                recordUnsupportedTrace(parsed, unsupported, describeUnsupported(element, relationships));
            }
        }
        return join(pieces, "");
    }

    private String extractTextboxText(Element textbox, Map<String, Relationship> relationships, Map<String, StyleDefinition> styles,
            Numbering numbering, ParsedDocx parsed, Set<String> knownAnchorIds, List<String> unsupported) {
        List<String> parts = new ArrayList<String>();
        for (Element paragraph : XmlUtils.childrenByLocalName(textbox, "p")) {
            String text = extractTextRuns(paragraph, relationships, styles, numbering, parsed, knownAnchorIds, unsupported, getParagraphTextStyle(paragraph, styles), false);
            if (text.length() > 0) {
                parts.add(renderStructuredParagraphText(paragraph, text, styles, numbering));
            }
        }
        return join(parts, "<br><br>").trim();
    }

    private void recordUnsupportedTrace(ParsedDocx parsed, List<String> unsupported, String type) {
        parsed.summary.recordUnsupported(type);
        unsupported.add(type);
    }

    private Map<String, Relationship> parseRelationships(byte[] bytes, String sourcePath) {
        Document document = XmlUtils.parseXml(bytes);
        Map<String, Relationship> relationships = new LinkedHashMap<String, Relationship>();
        for (Element element : XmlUtils.descendantsByLocalName(document, "Relationship")) {
            String id = XmlUtils.attr(element, "Id");
            Relationship relationship = new Relationship();
            relationship.type = XmlUtils.attr(element, "Type");
            relationship.mode = XmlUtils.attr(element, "TargetMode");
            String target = XmlUtils.attr(element, "Target");
            relationship.target = "External".equals(relationship.mode) ? target : resolveZipPath(sourcePath, target);
            relationships.put(id, relationship);
        }
        return relationships;
    }

    private String resolveZipPath(String sourcePath, String target) {
        if (target == null || target.length() == 0 || target.startsWith("#")) {
            return target == null ? "" : target;
        }
        if (target.startsWith("/")) {
            return target.replaceFirst("^/+", "");
        }
        List<String> parts = new ArrayList<String>();
        String[] base = sourcePath.split("/");
        for (int index = 0; index < base.length - 1; index++) {
            parts.add(base[index]);
        }
        for (String part : target.split("/")) {
            if (part.length() == 0 || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            } else {
                parts.add(part);
            }
        }
        return join(parts, "/");
    }

    private Map<String, StyleDefinition> parseStyles(byte[] bytes) {
        Map<String, StyleDefinition> styles = new LinkedHashMap<String, StyleDefinition>();
        if (bytes == null) {
            return styles;
        }
        Document document = XmlUtils.parseXml(bytes);
        for (Element styleElement : XmlUtils.descendantsByLocalName(document, "style")) {
            String styleId = XmlUtils.attr(styleElement, "styleId");
            if (styleId.length() == 0) {
                continue;
            }
            StyleDefinition style = new StyleDefinition();
            style.styleId = styleId;
            style.styleType = XmlUtils.attr(styleElement, "type");
            style.name = XmlUtils.attr(XmlUtils.firstChild(styleElement, "name"), "val");
            style.basedOn = XmlUtils.attr(XmlUtils.firstChild(styleElement, "basedOn"), "val");
            Element pPr = XmlUtils.firstChild(styleElement, "pPr");
            Element rPr = XmlUtils.firstChild(styleElement, "rPr");
            style.outlineLevel = parseInteger(XmlUtils.attr(XmlUtils.firstChild(pPr, "outlineLvl"), "val"));
            style.bold = readStyleFlag(rPr, "b");
            style.italic = readStyleFlag(rPr, "i");
            style.strike = readStyleFlag(rPr, "strike");
            style.underline = readStyleFlag(rPr, "u");
            styles.put(styleId, style);
        }
        return styles;
    }

    private Numbering parseNumbering(byte[] bytes) {
        Numbering numbering = new Numbering();
        if (bytes == null) {
            return numbering;
        }
        Document document = XmlUtils.parseXml(bytes);
        for (Element abstractNumElement : XmlUtils.descendantsByLocalName(document, "abstractNum")) {
            String abstractNumId = XmlUtils.attr(abstractNumElement, "abstractNumId");
            if (abstractNumId.length() == 0) {
                continue;
            }
            Map<Integer, String> levels = new HashMap<Integer, String>();
            for (Element levelElement : XmlUtils.childrenByLocalName(abstractNumElement, "lvl")) {
                Integer level = parseInteger(XmlUtils.attr(levelElement, "ilvl"));
                if (level == null) {
                    continue;
                }
                String format = XmlUtils.attr(XmlUtils.firstChild(levelElement, "numFmt"), "val");
                levels.put(level, "bullet".equals(format) ? "bullet" : "ordered");
            }
            numbering.abstractNums.put(abstractNumId, levels);
        }
        for (Element numElement : XmlUtils.descendantsByLocalName(document, "num")) {
            String numId = XmlUtils.attr(numElement, "numId");
            String abstractNumId = XmlUtils.attr(XmlUtils.firstChild(numElement, "abstractNumId"), "val");
            if (numId.length() > 0 && abstractNumId.length() > 0) {
                numbering.nums.put(numId, abstractNumId);
            }
        }
        return numbering;
    }

    private Integer getHeadingLevel(Element paragraph, Map<String, StyleDefinition> styles) {
        Element pPr = XmlUtils.firstChild(paragraph, "pPr");
        if (pPr == null) {
            return null;
        }
        String styleId = XmlUtils.attr(XmlUtils.firstChild(pPr, "pStyle"), "val");
        if (styleId.length() > 0) {
            Integer direct = headingNameLevel(styleId);
            if (direct != null) {
                return direct;
            }
            for (StyleDefinition style : resolveStyleChain(styles, styleId)) {
                Integer byName = headingNameLevel(style.name);
                if (byName == null) {
                    byName = headingNameLevel(style.styleId);
                }
                if (byName != null) {
                    return byName;
                }
                if (style.outlineLevel != null) {
                    return Math.min(style.outlineLevel.intValue() + 1, 6);
                }
            }
        }
        Integer outline = parseInteger(XmlUtils.attr(XmlUtils.firstChild(pPr, "outlineLvl"), "val"));
        return outline == null ? null : Math.min(outline.intValue() + 1, 6);
    }

    private ListMetadata getListMetadata(Element paragraph, Numbering numbering) {
        Element pPr = XmlUtils.firstChild(paragraph, "pPr");
        Element numPr = XmlUtils.firstChild(pPr, "numPr");
        if (numPr == null) {
            return null;
        }
        String numId = XmlUtils.attr(XmlUtils.firstChild(numPr, "numId"), "val");
        int indent = parseIntegerOr(XmlUtils.attr(XmlUtils.firstChild(numPr, "ilvl"), "val"), 0);
        String abstractNumId = numbering.nums.get(numId);
        if (abstractNumId == null) {
            return null;
        }
        Map<Integer, String> levels = numbering.abstractNums.get(abstractNumId);
        if (levels == null || !levels.containsKey(Integer.valueOf(indent))) {
            return null;
        }
        ListMetadata metadata = new ListMetadata();
        metadata.indent = indent;
        metadata.kind = levels.get(Integer.valueOf(indent));
        return metadata;
    }

    private String renderStructuredParagraphText(Element paragraph, String text, Map<String, StyleDefinition> styles, Numbering numbering) {
        ListMetadata list = getListMetadata(paragraph, numbering);
        if (list != null) {
            StringBuilder indent = new StringBuilder();
            for (int index = 0; index < Math.max(0, list.indent); index++) {
                indent.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            }
            return indent.toString() + ("ordered".equals(list.kind) ? "1." : "-") + " " + text;
        }
        Integer level = getHeadingLevel(paragraph, styles);
        if (level != null) {
            return repeat("#", Math.max(1, Math.min(level.intValue(), 6))) + " " + text;
        }
        return text;
    }

    private TextStyle getParagraphTextStyle(Element paragraph, Map<String, StyleDefinition> styles) {
        Element pPr = XmlUtils.firstChild(paragraph, "pPr");
        String paragraphStyleId = XmlUtils.attr(XmlUtils.firstChild(pPr, "pStyle"), "val");
        TextStyle style = applyStyleOverride(new TextStyle(), resolveTextStyleOverride(styles, paragraphStyleId, "paragraph"));
        return applyStyleOverride(style, readStyleOverride(XmlUtils.firstChild(pPr, "rPr")));
    }

    private TextStyle resolveRunTextStyle(Element run, Map<String, StyleDefinition> styles, TextStyle inherited, boolean suppressUnderline) {
        Element rPr = XmlUtils.firstChild(run, "rPr");
        String runStyleId = XmlUtils.attr(XmlUtils.firstChild(rPr, "rStyle"), "val");
        TextStyle style = applyStyleOverride(applyStyleOverride(inherited.copy(), resolveTextStyleOverride(styles, runStyleId, "character")), readStyleOverride(rPr));
        if (suppressUnderline) {
            style.underline = false;
        }
        return style;
    }

    private StyleOverride resolveTextStyleOverride(Map<String, StyleDefinition> styles, String styleId, String expectedType) {
        StyleOverride resolved = new StyleOverride();
        List<StyleDefinition> chain = resolveStyleChain(styles, styleId);
        for (int index = chain.size() - 1; index >= 0; index--) {
            StyleDefinition style = chain.get(index);
            if (expectedType != null && style.styleType != null && style.styleType.length() > 0 && !expectedType.equals(style.styleType)) {
                continue;
            }
            if (style.bold != null) {
                resolved.bold = style.bold;
            }
            if (style.italic != null) {
                resolved.italic = style.italic;
            }
            if (style.strike != null) {
                resolved.strike = style.strike;
            }
            if (style.underline != null) {
                resolved.underline = style.underline;
            }
        }
        return resolved;
    }

    private StyleOverride readStyleOverride(Element rPr) {
        StyleOverride override = new StyleOverride();
        override.bold = readStyleFlag(rPr, "b");
        override.italic = readStyleFlag(rPr, "i");
        override.strike = readStyleFlag(rPr, "strike");
        override.underline = readStyleFlag(rPr, "u");
        return override;
    }

    private TextStyle applyStyleOverride(TextStyle base, StyleOverride override) {
        if (override.bold != null) {
            base.bold = override.bold.booleanValue();
        }
        if (override.italic != null) {
            base.italic = override.italic.booleanValue();
        }
        if (override.strike != null) {
            base.strike = override.strike.booleanValue();
        }
        if (override.underline != null) {
            base.underline = override.underline.booleanValue();
        }
        return base;
    }

    private Boolean readStyleFlag(Element parent, String localName) {
        Element element = XmlUtils.firstChild(parent, localName);
        if (element == null) {
            return null;
        }
        String value = XmlUtils.attr(element, "val");
        return Boolean.valueOf(value.length() == 0 || (!"false".equals(value) && !"0".equals(value)));
    }

    private String applyTextStyle(String text, TextStyle style) {
        if (text == null || text.length() == 0) {
            return "";
        }
        String result = text;
        if (style.underline) {
            result = "<ins>" + result + "</ins>";
        }
        if (style.strike) {
            result = "~~" + result + "~~";
        }
        if (style.italic) {
            result = "*" + result + "*";
        }
        if (style.bold) {
            result = "**" + result + "**";
        }
        return result;
    }

    private String renderHyperlink(Element hyperlink, String linkText, Map<String, Relationship> relationships, ParsedDocx parsed, Set<String> knownAnchorIds) {
        String relationshipId = XmlUtils.namespacedAttr(hyperlink, "r", "id");
        String anchor = normalizeAnchorName(XmlUtils.attr(hyperlink, "anchor"));
        Relationship relationship = relationshipId.length() == 0 ? null : relationships.get(relationshipId);
        String relationshipAnchor = relationship == null ? "" : normalizeRelationshipAnchorTarget(relationship.target);
        if (relationship != null && "External".equals(relationship.mode)) {
            parsed.summary.links++;
            parsed.summary.externalLinks++;
            return "[" + linkText + "](" + relationship.target + ")";
        }
        if (relationshipAnchor.length() > 0 && knownAnchorIds.contains(relationshipAnchor)) {
            parsed.summary.links++;
            parsed.summary.internalLinks++;
            return "[" + linkText + "](#" + relationshipAnchor + ")";
        }
        if (anchor.length() > 0 && knownAnchorIds.contains(anchor)) {
            parsed.summary.links++;
            parsed.summary.internalLinks++;
            return "[" + linkText + "](#" + anchor + ")";
        }
        return linkText;
    }

    private String describeUnsupported(Element element, Map<String, Relationship> relationships) {
        String local = XmlUtils.localName(element);
        String type;
        if ("drawing".equals(local) || "pict".equals(local) || "object".equals(local)) {
            type = "drawing";
        } else if ("txbxContent".equals(local) || "textbox".equals(local) || "textBox".equals(local)) {
            type = "textbox";
        } else if ("chart".equals(local)) {
            type = "chart";
        } else {
            type = local.length() == 0 ? "unknown" : local;
        }
        if ("drawing".equals(type)) {
            String imageTarget = resolveImageTarget(element, relationships);
            if (imageTarget.length() > 0) {
                String trace = "drawing:image(" + imageTarget + ")";
                String alt = resolveImageAltText(element);
                String extent = resolveImageExtent(element);
                if (alt.length() > 0) {
                    trace += ":alt(" + alt + ")";
                }
                if (extent.length() > 0) {
                    trace += ":size-emu(" + extent + ")";
                }
                return trace;
            }
        }
        return type;
    }

    private String resolveImageTarget(Element element, Map<String, Relationship> relationships) {
        for (Element blip : XmlUtils.descendantsByLocalName(element, "blip")) {
            String id = XmlUtils.namespacedAttr(blip, "r", "embed");
            Relationship relationship = relationships.get(id);
            if (relationship != null && relationship.type != null && relationship.type.contains("/image")) {
                return relationship.target;
            }
        }
        return "";
    }

    private String resolveImageAltText(Element element) {
        List<Element> metadata = new ArrayList<Element>();
        metadata.addAll(XmlUtils.descendantsByLocalName(element, "docPr"));
        metadata.addAll(XmlUtils.descendantsByLocalName(element, "cNvPr"));
        for (Element meta : metadata) {
            String description = XmlUtils.attr(meta, "descr").trim();
            if (description.length() > 0) {
                return description;
            }
            String title = XmlUtils.attr(meta, "title").trim();
            if (title.length() > 0) {
                return title;
            }
        }
        return "";
    }

    private String resolveImageExtent(Element element) {
        for (Element extent : XmlUtils.descendantsByLocalName(element, "extent")) {
            String cx = XmlUtils.attr(extent, "cx").trim();
            String cy = XmlUtils.attr(extent, "cy").trim();
            if (cx.length() > 0 && cy.length() > 0) {
                return cx + "x" + cy;
            }
        }
        return "";
    }

    private void collectImageAssets(ParsedDocx parsed, Map<String, byte[]> files, byte[] contentTypesBytes) {
        ContentTypes contentTypes = parseContentTypes(contentTypesBytes);
        Set<String> seen = new HashSet<String>();
        for (int blockIndex = 0; blockIndex < parsed.blocks.size(); blockIndex++) {
            ParsedBlock block = parsed.blocks.get(blockIndex);
            List<String> traces = new ArrayList<String>();
            if ("unsupported".equals(block.kind)) {
                traces.add(block.type);
            }
            traces.addAll(block.unsupportedTypes);
            for (int traceIndex = 0; traceIndex < traces.size(); traceIndex++) {
                ImageTrace trace = parseImageTrace(traces.get(traceIndex));
                if (trace == null) {
                    continue;
                }
                String safePath = getSafeDocxAssetPath(trace.sourcePath);
                if (safePath.length() == 0 || seen.contains(safePath) || !files.containsKey(safePath)) {
                    continue;
                }
                seen.add(safePath);
                ParsedImageAsset asset = new ParsedImageAsset();
                asset.sourcePath = safePath;
                asset.mediaType = resolveImageMediaType(safePath, contentTypes);
                asset.altText = trace.altText;
                asset.sourceTrace = traces.get(traceIndex);
                asset.blockIndex = blockIndex;
                asset.traceIndex = traceIndex;
                asset.blockKind = block.kind;
                asset.bytes = files.get(safePath);
                parsed.assets.add(asset);
            }
        }
    }

    private ImageTrace parseImageTrace(String type) {
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
        String altMarker = ":alt(";
        int altStart = type.indexOf(altMarker, end);
        if (altStart >= 0) {
            int altEnd = type.indexOf(')', altStart + altMarker.length());
            if (altEnd >= 0) {
                trace.altText = type.substring(altStart + altMarker.length(), altEnd);
            }
        }
        if (trace.altText == null) {
            trace.altText = "";
        }
        return trace;
    }

    private ContentTypes parseContentTypes(byte[] bytes) {
        ContentTypes result = new ContentTypes();
        if (bytes == null) {
            return result;
        }
        Document document = XmlUtils.parseXml(bytes);
        for (Element element : XmlUtils.descendantsByLocalName(document, "Default")) {
            String extension = XmlUtils.attr(element, "Extension").trim().toLowerCase();
            String contentType = XmlUtils.attr(element, "ContentType").trim();
            if (extension.length() > 0 && contentType.length() > 0) {
                result.defaults.put(extension, contentType);
            }
        }
        for (Element element : XmlUtils.descendantsByLocalName(document, "Override")) {
            String partName = normalizePackagePath(XmlUtils.attr(element, "PartName").trim());
            String contentType = XmlUtils.attr(element, "ContentType").trim();
            if (partName.length() > 0 && contentType.length() > 0) {
                result.overrides.put(partName, contentType);
            }
        }
        return result;
    }

    private String resolveImageMediaType(String sourcePath, ContentTypes contentTypes) {
        String normalized = normalizePackagePath(sourcePath);
        if (contentTypes.overrides.containsKey(normalized)) {
            return contentTypes.overrides.get(normalized);
        }
        String extension = extension(normalized);
        if (contentTypes.defaults.containsKey(extension)) {
            return contentTypes.defaults.get(extension);
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalized.endsWith(".gif")) {
            return "image/gif";
        }
        if (normalized.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        if (normalized.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (normalized.endsWith(".tif") || normalized.endsWith(".tiff")) {
            return "image/tiff";
        }
        return "application/octet-stream";
    }

    private String getSafeDocxAssetPath(String sourcePath) {
        String normalized = normalizePackagePath(sourcePath);
        if (normalized.length() == 0 || normalized.startsWith("../") || normalized.contains("/../") || normalized.contains("..\\")) {
            return "";
        }
        return normalized;
    }

    private Set<String> collectKnownAnchorIds(Element body) {
        Set<String> anchors = new HashSet<String>();
        for (Element paragraph : XmlUtils.childrenByLocalName(body, "p")) {
            anchors.addAll(extractParagraphAnchors(paragraph));
        }
        return anchors;
    }

    private List<String> extractParagraphAnchors(Element paragraph) {
        List<String> anchors = new ArrayList<String>();
        for (Element bookmark : XmlUtils.childrenByLocalName(paragraph, "bookmarkStart")) {
            String anchor = normalizeAnchorName(XmlUtils.attr(bookmark, "name"));
            if (anchor.length() > 0) {
                anchors.add(anchor);
            }
        }
        return anchors;
    }

    private List<String> claimUniqueAnchors(List<String> anchors, Set<String> emitted) {
        List<String> unique = new ArrayList<String>();
        for (String anchor : anchors) {
            if (!emitted.contains(anchor)) {
                emitted.add(anchor);
                unique.add(anchor);
            }
        }
        return unique;
    }

    private String normalizeAnchorName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() == 0 || trimmed.startsWith("_")) {
            return "";
        }
        String lower = trimmed.toLowerCase();
        String normalized = lower.replaceAll("[^a-z0-9_\\-\\s]", "-").replaceAll("\\s+", "-").replaceAll("-+", "-");
        while (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeRelationshipAnchorTarget(String target) {
        if (target == null) {
            return "";
        }
        int hash = target.indexOf('#');
        return hash >= 0 ? normalizeAnchorName(target.substring(hash + 1)) : "";
    }

    private Element firstDescendant(Node node, String localName) {
        List<Element> descendants = XmlUtils.descendantsByLocalName(node, localName);
        return descendants.isEmpty() ? null : descendants.get(0);
    }

    private Integer headingNameLevel(String name) {
        if (name == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(Heading|見出し)\\s*([1-6])$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(name.trim());
        return matcher.matches() ? Integer.valueOf(Integer.parseInt(matcher.group(2))) : null;
    }

    private List<StyleDefinition> resolveStyleChain(Map<String, StyleDefinition> styles, String styleId) {
        List<StyleDefinition> chain = new ArrayList<StyleDefinition>();
        Set<String> visited = new HashSet<String>();
        String cursor = styleId;
        while (cursor != null && cursor.length() > 0 && styles.containsKey(cursor) && !visited.contains(cursor)) {
            visited.add(cursor);
            StyleDefinition style = styles.get(cursor);
            chain.add(style);
            cursor = style.basedOn;
        }
        return chain;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseIntegerOr(String value, int fallback) {
        Integer parsed = parseInteger(value);
        return parsed == null ? fallback : parsed.intValue();
    }

    private int getGridSpan(Element cell) {
        String value = XmlUtils.attr(XmlUtils.firstChild(XmlUtils.firstChild(cell, "tcPr"), "gridSpan"), "val");
        int parsed = parseIntegerOr(value, 1);
        return parsed > 0 ? parsed : 1;
    }

    private String getVerticalMergeState(Element cell) {
        Element verticalMerge = XmlUtils.firstChild(XmlUtils.firstChild(cell, "tcPr"), "vMerge");
        if (verticalMerge == null) {
            return null;
        }
        String value = XmlUtils.attr(verticalMerge, "val");
        if (value.length() == 0 || "continue".equals(value)) {
            return "continue";
        }
        return "restart".equals(value) ? "restart" : null;
    }

    private void normalizeRows(List<List<String>> rows) {
        int columns = 0;
        for (List<String> row : rows) {
            columns = Math.max(columns, row.size());
        }
        for (List<String> row : rows) {
            while (row.size() < columns) {
                row.add("");
            }
        }
    }

    private String normalizeInlineText(String text) {
        return (text == null ? "" : text).replace("\t", "    ").replaceAll(" {2,}", " ").trim();
    }

    private String normalizePackagePath(String path) {
        return (path == null ? "" : path).replaceFirst("^/+", "");
    }

    private String extension(String path) {
        String segment = path.substring(path.lastIndexOf('/') + 1);
        int index = segment.lastIndexOf('.');
        return index < 0 ? "" : segment.substring(index + 1).toLowerCase();
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private String join(List<String> parts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                builder.append(separator);
            }
            builder.append(parts.get(index));
        }
        return builder.toString();
    }

    private static class Numbering {
        Map<String, Map<Integer, String>> abstractNums = new HashMap<String, Map<Integer, String>>();
        Map<String, String> nums = new HashMap<String, String>();
    }

    private static class ListMetadata {
        String kind;
        int indent;
    }

    private static class StyleOverride {
        Boolean bold;
        Boolean italic;
        Boolean strike;
        Boolean underline;
    }

    private static class ImageTrace {
        String sourcePath;
        String altText;
    }

    private static class ContentTypes {
        Map<String, String> defaults = new HashMap<String, String>();
        Map<String, String> overrides = new HashMap<String, String>();
    }
}
