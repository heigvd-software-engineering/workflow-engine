package com.heig.entities.workflow.types;

import com.heig.entities.workflow.file.FileWrapper;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

/**
 * Type representing a file
 */
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

    /**
     * Returns the directory from a file
     * Example : <br>
     * A file name "dir/test.jpg" will give "dir/test"
     * @param file The file
     * @return The directory obtained from the file path
     */
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

        //Creates the cache directory named after the output file name
        var dirForFile = getDirForFile(output);
        if (dirForFile.exists()) {
            Utils.deleteCompleteDirectory(dirForFile);
        }
        if (!dirForFile.mkdirs()) {
            throw new RuntimeException("Could not create directory to cache the file");
        }

        //If the file is null we stop here
        if (fw.isFileNull()) {
            return;
        }

        //We write the file relative path to the cache
        WType.super.toFile(new File(dirForFile, "file.path"), fw.getFilePath());

        //If the file exists, we save it's content to the cache
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

        //If the file containing the cache doest not exist, it means that the file was null
        if (pathOpt.isEmpty() || !(pathOpt.get() instanceof String path)) {
            return Optional.of(FileWrapper.NONE);
        }

        var fileWrapper = new FileWrapper(path);
        if (dataOpt.isPresent() && dataOpt.get() instanceof String data) {
            //If the "file.obj" file exists, get replace the file if it already exists and write the content that was saved in the cache
            if (!fileWrapper.createOrReplace()) {
                throw new RuntimeException("Failed to create or replace existing file");
            }
            try (var writer = fileWrapper.writer()) {
                writer.write(data);
            }
        } else {
            //If the "file.obj" file doesn't exist, that means that the file path was specified but the file didn't exist
            //We remove the file if it exists
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
