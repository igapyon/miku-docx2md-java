package jp.igapyon.mikudocx2md.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdBatchOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdBatchResult;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdConversionException;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileConverter;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileOptions;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdFileResult;
import jp.igapyon.mikudocx2md.core.MikuDocx2mdCore;

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
        MikuDocx2mdFileOptions fileOptions = new MikuDocx2mdFileOptions();
        fileOptions.inputFile = Paths.get(options.inputPath);
        fileOptions.outputFile = options.outPath == null ? null : Paths.get(options.outPath);
        fileOptions.summaryFile = options.summaryOutPath == null ? null : Paths.get(options.summaryOutPath);
        fileOptions.assetsDirectory = options.assetsDir == null ? null : Paths.get(options.assetsDir);
        fileOptions.summaryToStdout = options.summary;
        fileOptions.includeUnsupportedComments = options.includeUnsupportedComments;
        fileOptions.frontMatter = options.frontMatter;
        fileOptions.listener = verbose;
        try {
            MikuDocx2mdFileResult result = new MikuDocx2mdFileConverter().convertFile(fileOptions);
            if (options.summary) {
                out.println(result.summary);
                verbose.log("summary-written stdout");
            }
            if (options.outPath == null) {
                out.print(result.markdown);
                verbose.log("markdown-written stdout");
            }
            verbose.log("done total-ms=" + (System.currentTimeMillis() - startedAt));
            return 0;
        } catch (MikuDocx2mdConversionException ex) {
            err.println(formatDocumentError(ex));
            return 1;
        }
    }

    private int runBatch(CliOptions options, PrintStream err, long startedAt) {
        if (options.outPath != null || options.summary || options.summaryOutPath != null) {
            err.println("--out, --summary, and --summary-out are not available with multiple input files or --input-directory.");
            return 1;
        }
        MikuDocx2mdBatchOptions batchOptions = new MikuDocx2mdBatchOptions();
        batchOptions.inputDirectory = options.inputDirectory == null ? null : Paths.get(options.inputDirectory);
        batchOptions.outputDirectory = options.outputDirectory == null ? null : Paths.get(options.outputDirectory);
        batchOptions.assetsDirectory = options.assetsDir == null ? null : Paths.get(options.assetsDir);
        batchOptions.recursive = options.recursive;
        batchOptions.includeUnsupportedComments = options.includeUnsupportedComments;
        batchOptions.frontMatter = options.frontMatter;
        for (String inputPath : options.inputPaths) {
            batchOptions.inputFiles.add(Paths.get(inputPath));
        }
        batchOptions.listener = new Verbose(options.verbose, startedAt, err);
        MikuDocx2mdBatchResult result;
        try {
            result = new MikuDocx2mdFileConverter().convertBatch(batchOptions);
        } catch (IOException ex) {
            err.println("input scan failed: " + ex.getMessage());
            return 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return 1;
        } catch (MikuDocx2mdConversionException ex) {
            err.println(formatDocumentError(ex));
            return 1;
        }
        if (result.getConvertedCount() == 0) {
            Path inputDirectory = options.inputDirectory == null ? null : Paths.get(options.inputDirectory);
            err.println("No .docx files found under " + (inputDirectory == null ? "input files" : inputDirectory));
            return 1;
        }
        batchOptions.listener.onEvent("batch-written count=" + result.getConvertedCount());
        batchOptions.listener.onEvent("done total-ms=" + (System.currentTimeMillis() - startedAt));
        return 0;
    }

    private boolean isBatchMode(CliOptions options) {
        return options.inputDirectory != null || options.inputPaths.size() > 1;
    }

    private String formatDocumentError(MikuDocx2mdConversionException error) {
        Path inputFile = error.getInputFile();
        String inputPath = inputFile == null ? null : inputFile.toString();
        String name = inputPath == null ? "input.docx" : Paths.get(inputPath).getFileName().toString();
        return "[" + name + "] " + error.getStage() + ": " + error.getMessage();
    }

    private void printHelp(PrintStream out) {
        out.println("miku-docx2md - local-first DOCX to Markdown converter");
        out.println();
        out.println("USAGE");
        out.println("  java -jar miku-docx2md-1.2.1.jar <input.docx> [options]");
        out.println("  java -jar miku-docx2md-1.2.1.jar --version");
        out.println("  java -jar miku-docx2md-1.2.1.jar --help");
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
        out.println("  --front-matter <mode>");
        out.println("      include or exclude. Default: include.");
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
        out.println("      Main converted document structure. Starts with YAML front matter by");
        out.println("      default; use --front-matter exclude to omit it.");
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
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md");
        out.println();
        out.println("  Print Markdown to stdout:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx");
        out.println();
        out.println("  Write Markdown and summary files:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md --summary-out ./sample.summary.txt");
        out.println();
        out.println("  Write Markdown and export image assets:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md --assets-dir ./sample.assets");
        out.println();
        out.println("  Include unsupported-element debug traces:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md --debug");
        out.println();
        out.println("  Omit YAML front matter:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md --front-matter exclude");
        out.println();
        out.println("  Show progress diagnostics on stderr:");
        out.println("    java -jar miku-docx2md-1.2.1.jar ./sample.docx --out ./sample.md --verbose");
        out.println();
        out.println("  Show version:");
        out.println("    java -jar miku-docx2md-1.2.1.jar --version");
        out.println();
        out.println("EXIT CODES");
        out.println("  0  Success, or explicit metadata command such as --version / --help.");
        out.println("  1  CLI usage error, file I/O error, parse error, or unexpected runtime error.");
        out.println();
    }

    private static class Verbose implements jp.igapyon.mikudocx2md.core.MikuDocx2mdConversionListener {
        private final boolean enabled;
        private final long startedAt;
        private final PrintStream err;

        Verbose(boolean enabled, long startedAt, PrintStream err) {
            this.enabled = enabled;
            this.startedAt = startedAt;
            this.err = err;
        }

        void log(String message) {
            onEvent(message);
        }

        @Override
        public void onEvent(String message) {
            if (enabled) {
                err.println("verbose: +" + (System.currentTimeMillis() - startedAt) + "ms " + message);
            }
        }
    }
}
