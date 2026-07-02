package jp.igapyon.mikudocx2md.core;

public class MarkdownOptions {
    public boolean includeUnsupportedComments;
    public String frontMatter = "exclude";
    public String title;
    public String toolVersion;
    public ImagePathResolver imagePathResolver;

    public interface ImagePathResolver {
        String resolve(String sourcePath);
    }
}
