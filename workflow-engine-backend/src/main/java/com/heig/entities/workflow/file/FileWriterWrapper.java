package com.heig.entities.workflow.file;

import com.heig.documentation.Document;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;

@Document("""
    This class is a wrapper for a FileWriter.
    By creating a new FileWriterWrapper (with the writer method of FileWrapper) you lock the file in write mode.
    /!\\ Always use close() when finished and don't reuse this instance after close. /!\\
    Code (in JS here) :
    ...
    let writer = file.writer();
    writer.write("test");
    writer.writeLine(" line");
    //Writes the line "test line" to the file
    writer.close();
    """)
public class FileWriterWrapper implements AutoCloseable {
    private final File file;
    private final FileManager.ReadWriteLock lock;
    FileWriterWrapper(@Nonnull File file) {
        this.file = file;
        lock = FileManager.INSTANCE.get(file.getAbsolutePath());
        lock.startWrite();
    }

    @Document("""
        Write the text to the file
        """)
    public void write(@Nonnull String text) {
        Objects.requireNonNull(text);

        try (var os = new BufferedWriter(new FileWriter(file, true))) {
            os.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Document("""
        Write the text with a newLine after to the file
        """)
    public void writeLine(@Nonnull String text) {
        Objects.requireNonNull(text);

        try (var os = new BufferedWriter(new FileWriter(file, true))) {
            os.write(text);
            os.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Document("""
        Releases write lock for this file.
        """)
    @Override
    public void close() {
        lock.endWrite();
    }
}
