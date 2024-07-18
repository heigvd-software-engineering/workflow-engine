package com.heig.entities.workflow.types;

import com.heig.entities.workflow.file.FileWrapper;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

public class WFile implements WType {
    private static final WFile instance = new WFile();

    private WFile() { }

    public static WFile of() {
        return instance;
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        return Objects.requireNonNull(other) instanceof WFile;
    }

    @Override
    public Object defaultValue() {
        return FileWrapper.NONE;
    }

    private static File getDirForFile(@Nonnull File file) {
        Objects.requireNonNull(file);

        var nodeCacheDir = file.getParentFile();
        var fileNameWithExt = file.getName();
        var indexOf = fileNameWithExt.lastIndexOf(".");
        var dirName = fileNameWithExt.substring(0, indexOf);
        return new File(nodeCacheDir, dirName);
    }

    @Override
    public void toFile(@Nonnull File output, @Nonnull Object value) {
        if (!(value instanceof FileWrapper fw)) {
            throw new RuntimeException("Value is not a FileWrapper");
        }

        var dirForFile = getDirForFile(output);
        if (dirForFile.exists()) {
            Utils.deleteCompleteDirectory(dirForFile);
        }
        if (!dirForFile.mkdirs()) {
            throw new RuntimeException("Could not create directory to cache the file");
        }

        if (fw.isFileNull()) {
            return;
        }

        WType.super.toFile(new File(dirForFile, "file.path"), fw.getFilePath());
        if (fw.exists()) {
            try (var reader = fw.reader()) {
                WType.super.toFile(new File(dirForFile, "file.obj"), reader.readAllText());
            }
        }
    }

    @Override
    public Optional<Object> fromFile(@Nonnull File input) {
        var dirForFile = getDirForFile(input);
        if (!dirForFile.exists()) {
            return Optional.empty();
        }

        var pathOpt = WType.super.fromFile(new File(dirForFile, "file.path"));
        var dataOpt = WType.super.fromFile(new File(dirForFile, "file.obj"));

        if (pathOpt.isEmpty() || !(pathOpt.get() instanceof String path)) {
            return Optional.empty();
        }

        var fileWrapper = new FileWrapper(path);
        if (dataOpt.isPresent() && dataOpt.get() instanceof String data) {
            if (!fileWrapper.createOrReplace()) {
                throw new RuntimeException("Failed to create or replace existing file");
            }
            try (var writer = fileWrapper.writer()) {
                writer.write(data);
            }
        } else {
            if (fileWrapper.exists()) {
                if (!fileWrapper.delete()) {
                    throw new RuntimeException("Could not delete file when loading from cache");
                }
            }
        }
        return Optional.of(fileWrapper);
    }

    @Override
    public int getHashCode(@Nonnull Object value) {
        if (value instanceof FileWrapper fw) {
            return fw.getContentHashCode();
        }
        throw new RuntimeException("WFile value should always be FileWrapper");
    }
}
