package jp.igapyon.mikudocx2md.cli;

import java.util.ArrayList;
import java.util.List;

class CliOptions {
    String inputPath;
    List<String> inputPaths = new ArrayList<String>();
    String inputDirectory;
    String outputDirectory;
    String outPath;
    String assetsDir;
    String summaryOutPath;
    String frontMatter = "include";
    boolean summary;
    boolean includeUnsupportedComments;
    boolean recursive;
    boolean verbose;
    boolean help;
    boolean version;

    static CliOptions parse(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            CliOptions options = new CliOptions();
            options.help = true;
            return options;
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            CliOptions options = new CliOptions();
            options.version = true;
            return options;
        }
        for (String arg : args) {
            if ("--help".equals(arg) || "--version".equals(arg)) {
                throw new IllegalArgumentException("Use --help or --version without other arguments.");
            }
        }

        CliOptions options = new CliOptions();
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (!arg.startsWith("--")) {
                options.inputPaths.add(arg);
                continue;
            }
            if ("--summary".equals(arg)) {
                options.summary = true;
            } else if ("--debug".equals(arg) || "--include-unsupported-comments".equals(arg)) {
                options.includeUnsupportedComments = true;
            } else if ("--recursive".equals(arg)) {
                options.recursive = true;
            } else if ("--verbose".equals(arg)) {
                options.verbose = true;
            } else if ("--out".equals(arg)) {
                options.outPath = requireValue(args, ++index, arg);
            } else if ("--assets-dir".equals(arg)) {
                options.assetsDir = requireValue(args, ++index, arg);
            } else if ("--summary-out".equals(arg)) {
                options.summaryOutPath = requireValue(args, ++index, arg);
            } else if ("--front-matter".equals(arg)) {
                options.frontMatter = requireFrontMatterMode(requireValue(args, ++index, arg));
            } else if ("--input-directory".equals(arg)) {
                options.inputDirectory = requireValue(args, ++index, arg);
            } else if ("--output-directory".equals(arg)) {
                options.outputDirectory = requireValue(args, ++index, arg);
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        if (options.inputPaths.size() == 1) {
            options.inputPath = options.inputPaths.get(0);
        }
        return options;
    }

    private static String requireFrontMatterMode(String value) {
        if ("include".equals(value) || "exclude".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Invalid front matter mode: " + value);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }
}
