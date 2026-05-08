package jp.igapyon.mikudocx2md.core;

import java.util.List;
import jp.igapyon.mikudocx2md.model.ParsedImageAsset;

final class AssetManifest {
    private AssetManifest() {
    }

    static String createAssetsManifestText(List<ParsedImageAsset> assets) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"version\": 1,\n  \"assets\": [");
        if (assets.isEmpty()) {
            builder.append("]\n}");
            return builder.toString();
        }
        for (int index = 0; index < assets.size(); index++) {
            ParsedImageAsset asset = assets.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("\n    {");
            builder.append("\n      \"kind\": \"image\",");
            builder.append("\n      \"sourcePath\": \"").append(json(asset.sourcePath)).append("\",");
            builder.append("\n      \"mediaType\": \"").append(json(asset.mediaType)).append("\",");
            builder.append("\n      \"altText\": \"").append(json(asset.altText)).append("\",");
            builder.append("\n      \"sourceTrace\": \"").append(json(asset.sourceTrace)).append("\",");
            builder.append("\n      \"blockIndex\": ").append(asset.blockIndex).append(',');
            builder.append("\n      \"documentPosition\": {");
            builder.append("\n        \"blockIndex\": ").append(asset.blockIndex).append(',');
            builder.append("\n        \"blockKind\": \"").append(json(asset.blockKind)).append("\",");
            builder.append("\n        \"traceIndex\": ").append(asset.traceIndex);
            builder.append("\n      },");
            builder.append("\n      \"size\": ").append(asset.bytes == null ? 0 : asset.bytes.length);
            builder.append("\n    }");
        }
        builder.append("\n  ]\n}");
        return builder.toString();
    }

    private static String json(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == '\\' || c == '"') {
                builder.append('\\').append(c);
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
