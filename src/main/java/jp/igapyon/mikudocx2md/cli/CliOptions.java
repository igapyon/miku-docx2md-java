package jp.igapyon.mikudocx2md.cli;

class CliOptions {
    String inputPath;
    String outPath;
    String assetsDir;
    String summaryOutPath;
    boolean summary;
    boolean includeUnsupportedComments;
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
        int positionals = 0;
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (!arg.startsWith("--")) {
                positionals++;
                if (positionals > 1) {
                    throw new IllegalArgumentException("Specify exactly one input .docx file.");
                }
                options.inputPath = arg;
                continue;
            }
            if ("--summary".equals(arg)) {
                options.summary = true;
            } else if ("--debug".equals(arg) || "--include-unsupported-comments".equals(arg)) {
                options.includeUnsupportedComments = true;
            } else if ("--verbose".equals(arg)) {
                options.verbose = true;
            } else if ("--out".equals(arg)) {
                options.outPath = requireValue(args, ++index, arg);
            } else if ("--assets-dir".equals(arg)) {
                options.assetsDir = requireValue(args, ++index, arg);
            } else if ("--summary-out".equals(arg)) {
                options.summaryOutPath = requireValue(args, ++index, arg);
            } else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        return options;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }
}
