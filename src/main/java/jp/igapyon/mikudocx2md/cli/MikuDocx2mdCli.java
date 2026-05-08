package jp.igapyon.mikudocx2md.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if (options.help || options.inputPath == null) {
            printHelp(out);
            return 0;
        }

        try {
            Verbose verbose = new Verbose(options.verbose, startedAt, err);
            verbose.log("reading input");
            byte[] bytes = Files.readAllBytes(Paths.get(options.inputPath));
            verbose.log("parsing docx");
            MikuDocx2mdCore core = new MikuDocx2mdCore();
            ParsedDocx parsed = core.parseDocx(bytes);
            MarkdownOptions markdownOptions = new MarkdownOptions();
            markdownOptions.includeUnsupportedComments = options.includeUnsupportedComments;
            markdownOptions.imagePathResolver = createImagePathResolver(options);
            String markdown = core.renderMarkdown(parsed, markdownOptions);

            if (options.outPath == null) {
                out.print(markdown);
                if (markdown.length() > 0) {
                    out.println();
                }
            } else {
                writeText(Paths.get(options.outPath), markdown);
            }

            if (options.assetsDir != null) {
                writeAssets(Paths.get(options.assetsDir), parsed, core);
            }

            String summary = core.createSummaryText(parsed);
            if (options.summary) {
                out.println(summary);
            }
            if (options.summaryOutPath != null) {
                writeText(Paths.get(options.summaryOutPath), summary);
            }
            verbose.log("completed");
            return 0;
        } catch (IOException ex) {
            err.println(formatDocumentError(options.inputPath, "read/write failed", ex));
            return 1;
        } catch (RuntimeException ex) {
            err.println(formatDocumentError(options.inputPath, "parse failed", ex));
            return 1;
        }
    }

    private MarkdownOptions.ImagePathResolver createImagePathResolver(final CliOptions options) {
        if (options.assetsDir == null) {
            return null;
        }
        return new MarkdownOptions.ImagePathResolver() {
            @Override
            public String resolve(String sourcePath) {
                if (options.outPath == null) {
                    return sourcePath;
                }
                Path outParent = Paths.get(options.outPath).toAbsolutePath().getParent();
                Path asset = Paths.get(options.assetsDir).toAbsolutePath().resolve(sourcePath);
                if (outParent == null) {
                    return sourcePath;
                }
                return outParent.relativize(asset).toString().replace('\\', '/');
            }
        };
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
        out.println("  java -jar miku-docx2md-0.9.0.jar <input.docx> [options]");
        out.println("  java -jar miku-docx2md-0.9.0.jar --version");
        out.println("  java -jar miku-docx2md-0.9.0.jar --help");
        out.println();
        out.println("CONTRACT");
        out.println("  Input is exactly one local .docx file path.");
        out.println("  Primary output is Markdown.");
        out.println("  If --out is set, Markdown is written to that file.");
        out.println("  If --out is omitted, Markdown is written to stdout.");
        out.println("  --summary prints conversion summary text to stdout.");
        out.println("  --summary-out writes conversion summary text to a file.");
        out.println("  --verbose writes progress and timing diagnostics to stderr.");
        out.println("  --help and --version are metadata commands and must be used without other arguments.");
        out.println();
        out.println("OPTIONS");
        out.println("  --out <file>");
        out.println("  --assets-dir <dir>");
        out.println("  --summary");
        out.println("  --summary-out <file>");
        out.println("  --debug");
        out.println("  --include-unsupported-comments");
        out.println("  --verbose");
        out.println("  --version");
        out.println("  --help");
        out.println();
        out.println("OUTPUTS");
        out.println("  Markdown, summary text, image assets, and manifest.json.");
        out.println();
        out.println("EXIT CODES");
        out.println("  0  Success, --version, or --help.");
        out.println("  1  CLI usage error, file I/O error, parse error, or runtime error.");
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
