package jp.igapyon.mikudocx2md.core;

public class MarkdownOptions {
    public boolean includeUnsupportedComments;
    public ImagePathResolver imagePathResolver;

    public interface ImagePathResolver {
        String resolve(String sourcePath);
    }
}
