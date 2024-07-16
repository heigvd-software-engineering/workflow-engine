package com.heig.entities.workflow.file;

import com.heig.documentation.Document;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.util.Arrays;

@Document("""
    This class is the wrapper for a file passed to an input / output of a node.
    Example :
    The node has 1 input of type file named "file".
    Code (in JS here) :
    let file = arguments.get("file");
    let reader = file.reader();
    //Do stuff
    reader.close();
    """)
public class FileWrapper {
    public static final File filesRootDirectory = new File(ConfigProvider.getConfig().getValue("files_directory", String.class));
    public static FileWrapper NONE = new FileWrapper(null);

    private final File file;
    private final String filePath;
    public FileWrapper(String filePath) {
        this.filePath = filePath;
        this.file = filePath == null ? null : new File(filesRootDirectory, filePath);
        //If the file exists and is not a file, we throw an exception
        if (file != null && file.exists() && !file.isFile()) {
            throw new IllegalStateException("File is not a file");
        }
    }

    private void assertFileNotNull() {
        if (isFileNull()) {
            throw new IllegalStateException("File is null");
        }
    }

    @Document("""
        Return true if the file is not valid.
        Can only occur if the input is optional.
        """)
    public boolean isFileNull() {
        return file == null;
    }

    @Document("""
        Returns true if the file exists and is a file, false otherwise.
        """)
    public boolean exists() {
        assertFileNotNull();

        return file.exists() && file.isFile();
    }

    @Document("""
        Create the file only if it doesn't exists.
        Returns false if the file already exists or the creation failed, true otherwise.
        """)
    public boolean create() {
        assertFileNotNull();

        if (exists()) {
            return false;
        }

        if (!ensureDirsCreated()) {
            return false;
        }

        try {
            return file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Document("""
        Create the file or replace it if it already exists.
        Returns false if the existing file could not be created or if the file creation failed, true otherwise.
        """)
    public boolean createOrReplace() {
        assertFileNotNull();

        if (exists()) {
            if (!delete()) {
                return false;
            }
        } else {
            if (!ensureDirsCreated()) {
                return false;
            }
        }
        return create();
    }

    @Document("""
        Deletes the file.
        Returns false if the file doesn't exist or if the deletion failed, true otherwise.
        """)
    public boolean delete() {
        assertFileNotNull();

        if (!exists()) {
            return false;
        }
        return file.delete();
    }

    /**
     * Ensures that all the parent directories of the file are created
     * @return
     */
    private boolean ensureDirsCreated() {
        assertFileNotNull();

        var parentDir = file.getParentFile();
        if (parentDir == null) {
            return true;
        }

        if (parentDir.exists()) {
            return true;
        }
        return parentDir.mkdirs();
    }

    @Document("""
        Returns the relative path of the file.
        """)
    public String getFilePath() {
        assertFileNotNull();

        return filePath;
    }

    @Document("""
        Return a custom FileReader.
        """)
    public FileReaderWrapper reader() {
        assertFileNotNull();

        return new FileReaderWrapper(file);
    }

    @Document("""
        Return a custom FileWriter.
        """)
    public FileWriterWrapper writer() {
        assertFileNotNull();

        return new FileWriterWrapper(file);
    }

    public int getContentHashCode() {
        if (isFileNull() || !exists()) {
            return 0;
        }

        try (var reader = reader()) {
            return Arrays.hashCode(new Object[] { reader.readAllText().hashCode(), filePath.hashCode() });
        }
    }
}
