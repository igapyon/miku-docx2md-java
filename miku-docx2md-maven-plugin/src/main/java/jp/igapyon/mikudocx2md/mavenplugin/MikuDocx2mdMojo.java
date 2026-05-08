package jp.igapyon.mikudocx2md.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import jp.igapyon.mikudocx2md.core.MarkdownOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import jp.igapyon.mikudocx2md.model.ParsedImageAsset;

@Mojo(name = "convert", threadSafe = true)
public class MikuDocx2mdMojo extends AbstractMojo {
    @Parameter(property = "miku-docx2md.inputFile", required = true)
    private File inputFile;

    @Parameter(property = "miku-docx2md.outputFile")
    private File outputFile;

    @Parameter(property = "miku-docx2md.summaryFile")
    private File summaryFile;

    @Parameter(property = "miku-docx2md.assetsDirectory")
    private File assetsDirectory;

    @Parameter(property = "miku-docx2md.includeUnsupportedComments", defaultValue = "false")
    private boolean includeUnsupportedComments;

    @Parameter(property = "miku-docx2md.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "miku-docx2md.verbose", defaultValue = "false")
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("miku-docx2md skipped.");
            return;
        }
        if (inputFile == null) {
            throw new MojoExecutionException("inputFile is required.");
        }
        final String documentName = inputFile.getName();
        final MikuDocx2mdCore core = new MikuDocx2mdCore();
        final ParsedDocx parsed;
        try {
            if (verbose) {
                getLog().info("miku-docx2md processing " + inputFile.getPath());
            }
            parsed = core.parseDocx(Files.readAllBytes(inputFile.toPath()));
        } catch (final IOException ex) {
            throw new MojoExecutionException(formatDocumentError(documentName, "read failed", ex), ex);
        } catch (final RuntimeException ex) {
            throw new MojoExecutionException(formatDocumentError(documentName, "parse failed", ex), ex);
        }

        final File actualOutputFile = outputFile == null ? new File(stripDocxExtension(inputFile.getName()) + ".md") : outputFile;
        final Path assetsPath = assetsDirectory == null ? null : assetsDirectory.toPath();
        final MarkdownOptions markdownOptions = new MarkdownOptions();
        markdownOptions.includeUnsupportedComments = includeUnsupportedComments;
        markdownOptions.imagePathResolver = createImagePathResolver(actualOutputFile.toPath(), assetsPath);
        final String markdown;
        final String summary;
        try {
            markdown = core.renderMarkdown(parsed, markdownOptions);
            summary = core.createSummaryText(parsed);
        } catch (final RuntimeException ex) {
            throw new MojoExecutionException(formatDocumentError(documentName, "convert failed", ex), ex);
        }

        try {
            writeText(actualOutputFile.toPath(), markdown);
            if (summaryFile != null) {
                writeText(summaryFile.toPath(), summary);
            }
            if (assetsPath != null) {
                writeAssets(assetsPath, parsed, core);
            }
        } catch (final IOException ex) {
            throw new MojoExecutionException(formatDocumentError(documentName, "write failed", ex), ex);
        }
        getLog().info("miku-docx2md wrote " + actualOutputFile.getPath());
    }

    static void writeText(final Path path, final String text) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static void writeAssets(final Path assetsDir, final ParsedDocx parsed, final MikuDocx2mdCore core) throws IOException {
        for (final ParsedImageAsset asset : parsed.assets) {
            final Path outputPath = assetsDir.resolve(asset.sourcePath).normalize();
            if (!outputPath.startsWith(assetsDir.normalize())) {
                throw new IOException("DOCX asset path escapes assets directory: " + asset.sourcePath);
            }
            final Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, asset.bytes);
        }
        writeText(assetsDir.resolve("manifest.json"), core.createAssetsManifestText(parsed));
    }

    static MarkdownOptions.ImagePathResolver createImagePathResolver(final Path outPath, final Path assetsDir) {
        if (assetsDir == null) {
            return null;
        }
        return new MarkdownOptions.ImagePathResolver() {
            @Override
            public String resolve(final String sourcePath) {
                if (outPath == null) {
                    return sourcePath;
                }
                final Path outParent = outPath.toAbsolutePath().getParent();
                final Path asset = assetsDir.toAbsolutePath().resolve(sourcePath);
                if (outParent == null) {
                    return sourcePath;
                }
                return outParent.relativize(asset).toString().replace('\\', '/');
            }
        };
    }

    static String stripDocxExtension(final String fileName) {
        return fileName.toLowerCase().endsWith(".docx") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    static String formatDocumentError(final String documentName, final String stage, final Throwable error) {
        final String message = error.getMessage() == null ? String.valueOf(error) : error.getMessage();
        return "[" + documentName + "] " + stage + ": " + message;
    }

    void setInputFile(final File inputFile) {
        this.inputFile = inputFile;
    }

    void setOutputFile(final File outputFile) {
        this.outputFile = outputFile;
    }

    void setSummaryFile(final File summaryFile) {
        this.summaryFile = summaryFile;
    }

    void setAssetsDirectory(final File assetsDirectory) {
        this.assetsDirectory = assetsDirectory;
    }

    void setIncludeUnsupportedComments(final boolean includeUnsupportedComments) {
        this.includeUnsupportedComments = includeUnsupportedComments;
    }

    void setSkip(final boolean skip) {
        this.skip = skip;
    }

    void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }
}
