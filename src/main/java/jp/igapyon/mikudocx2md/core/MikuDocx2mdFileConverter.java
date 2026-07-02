package jp.igapyon.mikudocx2md.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import jp.igapyon.mikudocx2md.model.ParsedDocx;
import jp.igapyon.mikudocx2md.model.ParsedImageAsset;

public class MikuDocx2mdFileConverter {
    private final MikuDocx2mdCore core;

    public MikuDocx2mdFileConverter() {
        this(new MikuDocx2mdCore());
    }

    public MikuDocx2mdFileConverter(MikuDocx2mdCore core) {
        this.core = core;
    }

    public MikuDocx2mdFileResult convertFile(MikuDocx2mdFileOptions options) throws MikuDocx2mdConversionException {
        if (options == null || options.inputFile == null) {
            throw new IllegalArgumentException("inputFile is required.");
        }
        MikuDocx2mdConversionListener listener = options.listener;
        log(listener, "input=" + options.inputFile);
        log(listener, "output=" + (options.outputFile == null ? "stdout" : options.outputFile));
        log(listener, "summary=" + (options.summaryFile == null ? (options.summaryToStdout ? "stdout" : "disabled") : options.summaryFile));
        log(listener, "assets=" + (options.assetsDirectory == null ? "disabled" : options.assetsDirectory));

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(options.inputFile);
        } catch (IOException ex) {
            throw new MikuDocx2mdConversionException(options.inputFile, "read failed", ex);
        }
        log(listener, "input-bytes=" + bytes.length);

        ParsedDocx parsed;
        String markdown;
        String summary;
        try {
            parsed = core.parseDocx(bytes);
            log(listener, "parsed blocks=" + parsed.blocks.size() + " assets=" + parsed.assets.size());
            MarkdownOptions markdownOptions = new MarkdownOptions();
            markdownOptions.includeUnsupportedComments = options.includeUnsupportedComments;
            markdownOptions.frontMatter = options.frontMatter;
            markdownOptions.title = options.inputFile.getFileName().toString();
            markdownOptions.toolVersion = MikuDocx2mdCore.VERSION;
            markdownOptions.imagePathResolver = createImagePathResolver(options.outputFile, options.assetsDirectory);
            markdown = core.renderMarkdown(parsed, markdownOptions);
            summary = core.createSummaryText(parsed);
        } catch (RuntimeException ex) {
            throw new MikuDocx2mdConversionException(options.inputFile, "parse failed", ex);
        }

        if (options.assetsDirectory != null) {
            try {
                writeAssets(options.assetsDirectory, parsed, core);
            } catch (IOException ex) {
                throw new MikuDocx2mdConversionException(options.inputFile, "asset write failed", ex);
            }
            log(listener, "assets-written count=" + parsed.assets.size());
        }
        if (options.summaryFile != null) {
            try {
                writeText(options.summaryFile, summary);
            } catch (IOException ex) {
                throw new MikuDocx2mdConversionException(options.inputFile, "summary write failed", ex);
            }
            log(listener, "summary-written " + options.summaryFile);
        }
        if (options.outputFile != null) {
            try {
                writeText(options.outputFile, markdown);
            } catch (IOException ex) {
                throw new MikuDocx2mdConversionException(options.inputFile, "markdown write failed", ex);
            }
            log(listener, "markdown-written " + options.outputFile);
        }

        MikuDocx2mdFileResult result = new MikuDocx2mdFileResult();
        result.inputFile = options.inputFile;
        result.outputFile = options.outputFile;
        result.summaryFile = options.summaryFile;
        result.assetsDirectory = options.assetsDirectory;
        result.markdown = markdown;
        result.summary = summary;
        result.inputBytes = bytes.length;
        result.blockCount = parsed.blocks.size();
        result.assetCount = parsed.assets.size();
        return result;
    }

    public MikuDocx2mdBatchResult convertBatch(MikuDocx2mdBatchOptions options)
            throws IOException, MikuDocx2mdConversionException {
        List<Path> inputFiles = collectBatchInputs(options);
        MikuDocx2mdBatchResult result = new MikuDocx2mdBatchResult();
        for (Path inputFile : inputFiles) {
            Path outputFile = resolveBatchOutputPath(options, inputFile);
            Path assetsDir = resolveBatchAssetsDir(options, inputFile);
            log(options.listener, "batch-input=" + inputFile);
            log(options.listener, "batch-output=" + outputFile);
            if (assetsDir != null) {
                log(options.listener, "batch-assets=" + assetsDir);
            }
            MikuDocx2mdFileOptions fileOptions = new MikuDocx2mdFileOptions();
            fileOptions.inputFile = inputFile;
            fileOptions.outputFile = outputFile;
            fileOptions.assetsDirectory = assetsDir;
            fileOptions.includeUnsupportedComments = options.includeUnsupportedComments;
            fileOptions.frontMatter = options.frontMatter;
            fileOptions.listener = options.listener;
            result.files.add(convertFile(fileOptions));
        }
        return result;
    }

    public List<Path> collectBatchInputs(MikuDocx2mdBatchOptions options) throws IOException {
        if (options == null) {
            throw new IllegalArgumentException("batch options are required.");
        }
        if (options.inputDirectory != null && !options.inputFiles.isEmpty()) {
            throw new IllegalArgumentException("--input-directory cannot be combined with positional input files.");
        }
        List<Path> inputs = new ArrayList<Path>();
        if (options.inputDirectory != null) {
            if (!Files.isDirectory(options.inputDirectory)) {
                throw new IllegalArgumentException("Input directory does not exist: " + options.inputDirectory);
            }
            int maxDepth = options.recursive ? Integer.MAX_VALUE : 1;
            try (Stream<Path> stream = Files.walk(options.inputDirectory, maxDepth)) {
                java.util.Iterator<Path> iterator = stream.iterator();
                while (iterator.hasNext()) {
                    Path candidate = iterator.next();
                    if (Files.isRegularFile(candidate) && candidate.getFileName().toString().toLowerCase().endsWith(".docx")) {
                        inputs.add(candidate);
                    }
                }
            }
        } else {
            inputs.addAll(options.inputFiles);
        }
        Collections.sort(inputs, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return inputs;
    }

    public Path resolveBatchOutputPath(MikuDocx2mdBatchOptions options, Path inputFile) {
        String outputName = stripDocxExtension(inputFile.getFileName().toString()) + ".md";
        if (options.outputDirectory == null) {
            Path parent = inputFile.getParent();
            return parent == null ? Paths.get(outputName) : parent.resolve(outputName);
        }
        if (options.inputDirectory == null) {
            return options.outputDirectory.resolve(outputName);
        }
        Path relative = options.inputDirectory.toAbsolutePath().normalize().relativize(inputFile.toAbsolutePath().normalize());
        Path relativeParent = relative.getParent();
        return relativeParent == null ? options.outputDirectory.resolve(outputName) : options.outputDirectory.resolve(relativeParent).resolve(outputName);
    }

    public Path resolveBatchAssetsDir(MikuDocx2mdBatchOptions options, Path inputFile) {
        if (options.assetsDirectory == null) {
            return null;
        }
        String assetsName = stripDocxExtension(inputFile.getFileName().toString()) + ".assets";
        if (options.inputDirectory == null) {
            return options.assetsDirectory.resolve(assetsName);
        }
        Path relative = options.inputDirectory.toAbsolutePath().normalize().relativize(inputFile.toAbsolutePath().normalize());
        Path relativeParent = relative.getParent();
        return relativeParent == null ? options.assetsDirectory.resolve(assetsName) : options.assetsDirectory.resolve(relativeParent).resolve(assetsName);
    }

    public static String stripDocxExtension(String fileName) {
        return fileName.toLowerCase().endsWith(".docx") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    public static MarkdownOptions.ImagePathResolver createImagePathResolver(final Path outPath, final Path assetsDir) {
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

    public static void writeAssets(Path assetsDir, ParsedDocx parsed, MikuDocx2mdCore core) throws IOException {
        for (ParsedImageAsset asset : parsed.assets) {
            Path outputPath = assetsDir.resolve(asset.sourcePath).normalize();
            if (!outputPath.startsWith(assetsDir.normalize())) {
                throw new IOException("DOCX asset path escapes assets directory: " + asset.sourcePath);
            }
            writeBytes(outputPath, asset.bytes);
        }
        writeText(assetsDir.resolve("manifest.json"), core.createAssetsManifestText(parsed));
    }

    public static void writeText(Path path, String text) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeBytes(Path path, byte[] bytes) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, bytes);
    }

    private static void log(MikuDocx2mdConversionListener listener, String message) {
        if (listener != null) {
            listener.onEvent(message);
        }
    }
}
