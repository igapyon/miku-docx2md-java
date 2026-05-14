package jp.igapyon.mikudocx2md.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import jp.igapyon.mikudocx2md.core.MarkdownOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import jp.igapyon.mikudocx2md.model.ParsedImageAsset;

public class MikuDocx2mdCli {
    public static void main(String[] args) {
        int exitCode = new MikuDocx2mdCli().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        long startedAt = System.currentTimeMillis();
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return 1;
        }

        if (options.version) {
            out.println("miku-docx2md " + MikuDocx2mdCore.VERSION);
            return 0;
        }
        if (options.help) {
            printHelp(out);
            return 0;
        }
        if (options.inputPath == null && options.inputDirectory == null && options.inputPaths.isEmpty()) {
            printHelp(out);
            return 1;
        }
        if (isBatchMode(options)) {
            return runBatch(options, err, startedAt);
        }

        Verbose verbose = new Verbose(options.verbose, startedAt, err);
        verbose.log("input=" + options.inputPath);
        verbose.log("output=" + (options.outPath == null ? "stdout" : options.outPath));
        verbose.log("summary=" + (options.summaryOutPath == null ? (options.summary ? "stdout" : "disabled") : options.summaryOutPath));
        verbose.log("assets=" + (options.assetsDir == null ? "disabled" : options.assetsDir));
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(options.inputPath));
        } catch (IOException ex) {
            err.println(formatDocumentError(options.inputPath, "read failed", ex));
            return 1;
        }
        verbose.log("input-bytes=" + bytes.length);

        try {
            MikuDocx2mdCore core = new MikuDocx2mdCore();
            ParsedDocx parsed = core.parseDocx(bytes);
            verbose.log("parsed blocks=" + parsed.blocks.size() + " assets=" + parsed.assets.size());
            MarkdownOptions markdownOptions = new MarkdownOptions();
            markdownOptions.includeUnsupportedComments = options.includeUnsupportedComments;
            markdownOptions.imagePathResolver = createImagePathResolver(options);
            String markdown = core.renderMarkdown(parsed, markdownOptions);
            String summary = core.createSummaryText(parsed);

            if (options.assetsDir != null) {
                try {
                    writeAssets(Paths.get(options.assetsDir), parsed, core);
                } catch (IOException ex) {
                    err.println(formatDocumentError(options.inputPath, "asset write failed", ex));
                    return 1;
                }
                verbose.log("assets-written count=" + parsed.assets.size());
            }

            if (options.summary) {
                out.println(summary);
                verbose.log("summary-written stdout");
            }
            if (options.summaryOutPath != null) {
                try {
                    writeText(Paths.get(options.summaryOutPath), summary);
                } catch (IOException ex) {
                    err.println(formatDocumentError(options.inputPath, "summary write failed", ex));
                    return 1;
                }
                verbose.log("summary-written " + options.summaryOutPath);
            }
            if (options.outPath == null) {
                out.print(markdown);
                verbose.log("markdown-written stdout");
            } else {
                try {
                    writeText(Paths.get(options.outPath), markdown);
                } catch (IOException ex) {
                    err.println(formatDocumentError(options.inputPath, "markdown write failed", ex));
                    return 1;
                }
                verbose.log("markdown-written " + options.outPath);
            }
            verbose.log("done total-ms=" + (System.currentTimeMillis() - startedAt));
            return 0;
        } catch (RuntimeException ex) {
            err.println(formatDocumentError(options.inputPath, "parse failed", ex));
            return 1;
        }
    }

    private int runBatch(CliOptions options, PrintStream err, long startedAt) {
        if (options.outPath != null || options.summary || options.summaryOutPath != null) {
            err.println("--out, --summary, and --summary-out are not available with multiple input files or --input-directory.");
            return 1;
        }
        final List<Path> inputFiles;
        try {
            inputFiles = collectBatchInputs(options);
        } catch (IOException ex) {
            err.println("input scan failed: " + ex.getMessage());
            return 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return 1;
        }
        if (inputFiles.isEmpty()) {
            Path inputDirectory = options.inputDirectory == null ? null : Paths.get(options.inputDirectory);
            err.println("No .docx files found under " + (inputDirectory == null ? "input files" : inputDirectory));
            return 1;
        }

        Verbose verbose = new Verbose(options.verbose, startedAt, err);
        int converted = 0;
        for (Path inputFile : inputFiles) {
            Path outputFile = resolveBatchOutputPath(options, inputFile);
            Path assetsDir = resolveBatchAssetsDir(options, inputFile, outputFile);
            verbose.log("batch-input=" + inputFile);
            verbose.log("batch-output=" + outputFile);
            if (assetsDir != null) {
                verbose.log("batch-assets=" + assetsDir);
            }
            int status = convertSingleFile(inputFile.toString(), outputFile, null, assetsDir, options.includeUnsupportedComments, err, verbose);
            if (status != 0) {
                return status;
            }
            converted++;
        }
        verbose.log("batch-written count=" + converted);
        verbose.log("done total-ms=" + (System.currentTimeMillis() - startedAt));
        return 0;
    }

    private int convertSingleFile(String inputPath, Path outPath, Path summaryOutPath, Path assetsDir, boolean includeUnsupportedComments,
            PrintStream err, Verbose verbose) {
        verbose.log("input=" + inputPath);
        verbose.log("output=" + (outPath == null ? "stdout" : outPath));
        verbose.log("summary=" + (summaryOutPath == null ? "disabled" : summaryOutPath));
        verbose.log("assets=" + (assetsDir == null ? "disabled" : assetsDir));
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(inputPath));
        } catch (IOException ex) {
            err.println(formatDocumentError(inputPath, "read failed", ex));
            return 1;
        }
        verbose.log("input-bytes=" + bytes.length);

        try {
            MikuDocx2mdCore core = new MikuDocx2mdCore();
            ParsedDocx parsed = core.parseDocx(bytes);
            verbose.log("parsed blocks=" + parsed.blocks.size() + " assets=" + parsed.assets.size());
            MarkdownOptions markdownOptions = new MarkdownOptions();
            markdownOptions.includeUnsupportedComments = includeUnsupportedComments;
            markdownOptions.imagePathResolver = createImagePathResolver(outPath, assetsDir);
            String markdown = core.renderMarkdown(parsed, markdownOptions);
            String summary = core.createSummaryText(parsed);

            if (assetsDir != null) {
                try {
                    writeAssets(assetsDir, parsed, core);
                } catch (IOException ex) {
                    err.println(formatDocumentError(inputPath, "asset write failed", ex));
                    return 1;
                }
                verbose.log("assets-written count=" + parsed.assets.size());
            }
            if (summaryOutPath != null) {
                try {
                    writeText(summaryOutPath, summary);
                } catch (IOException ex) {
                    err.println(formatDocumentError(inputPath, "summary write failed", ex));
                    return 1;
                }
                verbose.log("summary-written " + summaryOutPath);
            }
            if (outPath != null) {
                try {
                    writeText(outPath, markdown);
                } catch (IOException ex) {
                    err.println(formatDocumentError(inputPath, "markdown write failed", ex));
                    return 1;
                }
                verbose.log("markdown-written " + outPath);
            }
            return 0;
        } catch (RuntimeException ex) {
            err.println(formatDocumentError(inputPath, "parse failed", ex));
            return 1;
        }
    }

    private boolean isBatchMode(CliOptions options) {
        return options.inputDirectory != null || options.inputPaths.size() > 1;
    }

    private List<Path> collectBatchInputs(CliOptions options) throws IOException {
        if (options.inputDirectory != null && !options.inputPaths.isEmpty()) {
            throw new IllegalArgumentException("--input-directory cannot be combined with positional input files.");
        }
        List<Path> inputs = new ArrayList<Path>();
        if (options.inputDirectory != null) {
            Path inputDirectory = Paths.get(options.inputDirectory);
            if (!Files.isDirectory(inputDirectory)) {
                throw new IllegalArgumentException("Input directory does not exist: " + inputDirectory);
            }
            int maxDepth = options.recursive ? Integer.MAX_VALUE : 1;
            try (Stream<Path> stream = Files.walk(inputDirectory, maxDepth)) {
                java.util.Iterator<Path> iterator = stream.iterator();
                while (iterator.hasNext()) {
                    Path candidate = iterator.next();
                    if (Files.isRegularFile(candidate) && candidate.getFileName().toString().toLowerCase().endsWith(".docx")) {
                        inputs.add(candidate);
                    }
                }
            }
        } else {
            for (String inputPath : options.inputPaths) {
                inputs.add(Paths.get(inputPath));
            }
        }
        Collections.sort(inputs, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return inputs;
    }

    private Path resolveBatchOutputPath(CliOptions options, Path inputFile) {
        String outputName = stripDocxExtension(inputFile.getFileName().toString()) + ".md";
        if (options.outputDirectory == null) {
            Path parent = inputFile.getParent();
            return parent == null ? Paths.get(outputName) : parent.resolve(outputName);
        }
        Path outputDirectory = Paths.get(options.outputDirectory);
        if (options.inputDirectory == null) {
            return outputDirectory.resolve(outputName);
        }
        Path relative = Paths.get(options.inputDirectory).toAbsolutePath().normalize().relativize(inputFile.toAbsolutePath().normalize());
        Path relativeParent = relative.getParent();
        return relativeParent == null ? outputDirectory.resolve(outputName) : outputDirectory.resolve(relativeParent).resolve(outputName);
    }

    private Path resolveBatchAssetsDir(CliOptions options, Path inputFile, Path outputFile) {
        if (options.assetsDir == null) {
            return null;
        }
        Path assetsRoot = Paths.get(options.assetsDir);
        String assetsName = stripDocxExtension(inputFile.getFileName().toString()) + ".assets";
        if (options.inputDirectory == null) {
            return assetsRoot.resolve(assetsName);
        }
        Path relative = Paths.get(options.inputDirectory).toAbsolutePath().normalize().relativize(inputFile.toAbsolutePath().normalize());
        Path relativeParent = relative.getParent();
        return relativeParent == null ? assetsRoot.resolve(assetsName) : assetsRoot.resolve(relativeParent).resolve(assetsName);
    }

    private String stripDocxExtension(String fileName) {
        return fileName.toLowerCase().endsWith(".docx") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private MarkdownOptions.ImagePathResolver createImagePathResolver(final Path outPath, final Path assetsDir) {
        if (assetsDir == null) {
            return null;
        }
        return new MarkdownOptions.ImagePathResolver() {
            @Override
            public String resolve(String sourcePath) {
                if (outPath == null) {
                    return sourcePath;
                }
                Path outParent = outPath.toAbsolutePath().getParent();
                Path asset = assetsDir.toAbsolutePath().resolve(sourcePath);
                if (outParent == null) {
                    return sourcePath;
                }
                return outParent.relativize(asset).toString().replace('\\', '/');
            }
        };
    }

    private MarkdownOptions.ImagePathResolver createImagePathResolver(final CliOptions options) {
        return createImagePathResolver(options.outPath == null ? null : Paths.get(options.outPath),
                options.assetsDir == null ? null : Paths.get(options.assetsDir));
    }

    private void writeAssets(Path assetsDir, ParsedDocx parsed, MikuDocx2mdCore core) throws IOException {
        for (ParsedImageAsset asset : parsed.assets) {
            Path outputPath = assetsDir.resolve(asset.sourcePath).normalize();
            if (!outputPath.startsWith(assetsDir.normalize())) {
                throw new IOException("DOCX asset path escapes assets directory: " + asset.sourcePath);
            }
            writeBytes(outputPath, asset.bytes);
        }
        writeText(assetsDir.resolve("manifest.json"), core.createAssetsManifestText(parsed));
    }

    private void writeText(Path path, String text) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void writeBytes(Path path, byte[] bytes) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, bytes);
    }

    private String formatDocumentError(String inputPath, String stage, Exception error) {
        String name = inputPath == null ? "input.docx" : Paths.get(inputPath).getFileName().toString();
        return "[" + name + "] " + stage + ": " + error.getMessage();
    }

    private void printHelp(PrintStream out) {
        out.println("miku-docx2md - local-first DOCX to Markdown converter");
        out.println();
        out.println("USAGE");
        out.println("  java -jar miku-docx2md-1.0.0.jar <input.docx> [options]");
        out.println("  java -jar miku-docx2md-1.0.0.jar --version");
        out.println("  java -jar miku-docx2md-1.0.0.jar --help");
        out.println();
        out.println("CONTRACT");
        out.println("  Input is exactly one local .docx file path.");
        out.println("  Primary output is Markdown.");
        out.println("  If --out is set, Markdown is written to that file.");
        out.println("  If --out is omitted, Markdown is written to stdout.");
        out.println("  --summary prints conversion summary text to stdout.");
        out.println("  --summary-out writes conversion summary text to a file.");
        out.println("  If --out is omitted, avoid --summary unless mixed stdout output is acceptable.");
        out.println("  --verbose writes progress and timing diagnostics to stderr.");
        out.println("  --help and --version are metadata commands and must be used without other arguments.");
        out.println();
        out.println("OPTIONS");
        out.println("  --out <file>");
        out.println("      Write Markdown to this file. Parent directories are created.");
        out.println();
        out.println("  --assets-dir <dir>");
        out.println("      Export resolved embedded image assets into this directory.");
        out.println("      Also writes <dir>/manifest.json.");
        out.println("      Markdown image links are made relative to --out, or to the current directory");
        out.println("      when --out is omitted.");
        out.println();
        out.println("  --summary");
        out.println("      Print summary text to stdout.");
        out.println();
        out.println("  --summary-out <file>");
        out.println("      Write summary text to this file. Parent directories are created.");
        out.println();
        out.println("  --debug");
        out.println("      Include unsupported-element HTML comment traces in Markdown.");
        out.println();
        out.println("  --include-unsupported-comments");
        out.println("      Alias for --debug.");
        out.println();
        out.println("  --verbose");
        out.println("      Write progress and timing diagnostics to stderr with a \"verbose:\" prefix.");
        out.println("      Primary Markdown and summary outputs are unchanged.");
        out.println();
        out.println("  --version");
        out.println("      Show product name and package version, then exit.");
        out.println();
        out.println("  --help");
        out.println("      Show this help, then exit.");
        out.println();
        out.println("JAVA EXTENSIONS");
        out.println("  Multiple positional input files are converted as a batch.");
        out.println("  Batch conversion writes Markdown next to each input file, or under");
        out.println("  --output-directory when it is provided.");
        out.println();
        out.println("  --input-directory <dir>");
        out.println("      Convert .docx files under this directory.");
        out.println();
        out.println("  --output-directory <dir>");
        out.println("      Write batch Markdown files under this directory.");
        out.println();
        out.println("  --recursive");
        out.println("      Recursively scan --input-directory.");
        out.println();
        out.println("OUTPUTS");
        out.println("  Markdown:");
        out.println("      Main converted document structure.");
        out.println();
        out.println("  Summary:");
        out.println("      Text counts and diagnostics for converted document content.");
        out.println();
        out.println("  Asset directory:");
        out.println("      Contains resolved embedded image files at package-relative paths such as");
        out.println("      word/media/example.png, plus manifest.json.");
        out.println();
        out.println("  Asset manifest:");
        out.println("      JSON with asset path, media type, alt text, byte size, source trace,");
        out.println("      block index, and document position.");
        out.println();
        out.println("EXAMPLES");
        out.println("  Write Markdown to a file:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md");
        out.println();
        out.println("  Print Markdown to stdout:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx");
        out.println();
        out.println("  Write Markdown and summary files:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --summary-out ./sample.summary.txt");
        out.println();
        out.println("  Write Markdown and export image assets:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --assets-dir ./sample.assets");
        out.println();
        out.println("  Include unsupported-element debug traces:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --debug");
        out.println();
        out.println("  Show progress diagnostics on stderr:");
        out.println("    java -jar miku-docx2md-1.0.0.jar ./sample.docx --out ./sample.md --verbose");
        out.println();
        out.println("  Show version:");
        out.println("    java -jar miku-docx2md-1.0.0.jar --version");
        out.println();
        out.println("EXIT CODES");
        out.println("  0  Success, or explicit metadata command such as --version / --help.");
        out.println("  1  CLI usage error, file I/O error, parse error, or unexpected runtime error.");
        out.println();
    }

    private static class Verbose {
        private final boolean enabled;
        private final long startedAt;
        private final PrintStream err;

        Verbose(boolean enabled, long startedAt, PrintStream err) {
            this.enabled = enabled;
            this.startedAt = startedAt;
            this.err = err;
        }

        void log(String message) {
            if (enabled) {
                err.println("verbose: +" + (System.currentTimeMillis() - startedAt) + "ms " + message);
            }
        }
    }
}
