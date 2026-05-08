package jp.igapyon.mikudocx2md.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import jp.igapyon.mikudocx2md.core.MarkdownOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;
import jp.igapyon.mikudocx2md.model.ParsedDocx;

@Mojo(name = "convert-directory", threadSafe = true)
public class ConvertDirectoryMojo extends AbstractMojo {
    @Parameter(property = "miku-docx2md.inputDirectory", required = true)
    private File inputDirectory;

    @Parameter(property = "miku-docx2md.outputDirectory")
    private File outputDirectory;

    @Parameter(property = "miku-docx2md.assetsDirectory")
    private File assetsDirectory;

    @Parameter(property = "miku-docx2md.recursive", defaultValue = "false")
    private boolean recursive;

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
        final List<Path> inputs;
        try {
            inputs = collectInputs();
        } catch (final IOException ex) {
            throw new MojoExecutionException("input scan failed: " + ex.getMessage(), ex);
        }
        if (inputs.isEmpty()) {
            getLog().info("No .docx files found under " + (inputDirectory == null ? "inputDirectory" : inputDirectory.getPath()));
            return;
        }
        for (final Path input : inputs) {
            convert(input);
        }
    }

    private void convert(final Path input) throws MojoExecutionException {
        final MikuDocx2mdCore core = new MikuDocx2mdCore();
        final ParsedDocx parsed;
        try {
            if (verbose) {
                getLog().info("miku-docx2md processing " + input.toString());
            }
            parsed = core.parseDocx(Files.readAllBytes(input));
        } catch (final IOException ex) {
            throw new MojoExecutionException(MikuDocx2mdMojo.formatDocumentError(input.getFileName().toString(), "read failed", ex), ex);
        } catch (final RuntimeException ex) {
            throw new MojoExecutionException(MikuDocx2mdMojo.formatDocumentError(input.getFileName().toString(), "parse failed", ex), ex);
        }

        final Path output = resolveOutputPath(input);
        final Path assets = resolveAssetsPath(input);
        final MarkdownOptions options = new MarkdownOptions();
        options.includeUnsupportedComments = includeUnsupportedComments;
        options.imagePathResolver = MikuDocx2mdMojo.createImagePathResolver(output, assets);
        try {
            MikuDocx2mdMojo.writeText(output, core.renderMarkdown(parsed, options));
            if (assets != null) {
                MikuDocx2mdMojo.writeAssets(assets, parsed, core);
            }
        } catch (final IOException ex) {
            throw new MojoExecutionException(MikuDocx2mdMojo.formatDocumentError(input.getFileName().toString(), "write failed", ex), ex);
        }
        getLog().info("miku-docx2md wrote " + output.toString());
    }

    private List<Path> collectInputs() throws IOException, MojoExecutionException {
        if (inputDirectory == null) {
            throw new MojoExecutionException("inputDirectory is required.");
        }
        final Path root = inputDirectory.toPath();
        if (!Files.isDirectory(root)) {
            throw new MojoExecutionException("Input directory does not exist: " + root);
        }
        final List<Path> inputs = new ArrayList<Path>();
        final int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        try (Stream<Path> stream = Files.walk(root, maxDepth)) {
            final java.util.Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                final Path candidate = iterator.next();
                if (Files.isRegularFile(candidate) && candidate.getFileName().toString().toLowerCase().endsWith(".docx")) {
                    inputs.add(candidate);
                }
            }
        }
        Collections.sort(inputs, new Comparator<Path>() {
            @Override
            public int compare(final Path left, final Path right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return inputs;
    }

    private Path resolveOutputPath(final Path input) {
        final String outputName = MikuDocx2mdMojo.stripDocxExtension(input.getFileName().toString()) + ".md";
        if (outputDirectory == null) {
            final Path parent = input.getParent();
            return parent == null ? java.nio.file.Paths.get(outputName) : parent.resolve(outputName);
        }
        final Path root = inputDirectory.toPath().toAbsolutePath().normalize();
        final Path relative = root.relativize(input.toAbsolutePath().normalize());
        final Path relativeParent = relative.getParent();
        final Path outputRoot = outputDirectory.toPath();
        return relativeParent == null ? outputRoot.resolve(outputName) : outputRoot.resolve(relativeParent).resolve(outputName);
    }

    private Path resolveAssetsPath(final Path input) {
        if (assetsDirectory == null) {
            return null;
        }
        final String assetsName = MikuDocx2mdMojo.stripDocxExtension(input.getFileName().toString()) + ".assets";
        final Path root = inputDirectory.toPath().toAbsolutePath().normalize();
        final Path relative = root.relativize(input.toAbsolutePath().normalize());
        final Path relativeParent = relative.getParent();
        final Path assetsRoot = assetsDirectory.toPath();
        return relativeParent == null ? assetsRoot.resolve(assetsName) : assetsRoot.resolve(relativeParent).resolve(assetsName);
    }

    void setInputDirectory(final File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    void setAssetsDirectory(final File assetsDirectory) {
        this.assetsDirectory = assetsDirectory;
    }

    void setRecursive(final boolean recursive) {
        this.recursive = recursive;
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
