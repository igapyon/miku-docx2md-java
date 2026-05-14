package jp.igapyon.mikudocx2md.core;

import java.nio.file.Path;

public class MikuDocx2mdConversionException extends Exception {
    private final Path inputFile;
    private final String stage;

    public MikuDocx2mdConversionException(Path inputFile, String stage, Throwable cause) {
        super(cause == null ? null : cause.getMessage(), cause);
        this.inputFile = inputFile;
        this.stage = stage;
    }

    public Path getInputFile() {
        return inputFile;
    }

    public String getStage() {
        return stage;
    }
}
