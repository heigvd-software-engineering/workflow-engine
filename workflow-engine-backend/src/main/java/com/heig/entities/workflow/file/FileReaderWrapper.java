package com.heig.entities.workflow.file;

import com.heig.entities.documentation.Document;
import jakarta.annotation.Nonnull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Document("""
    This class is a wrapper for a FileReader.
    By creating a new FileReaderWrapper (with the reader method of FileWrapper) you lock the file in read mode.
    /!\\ Always use close() when finished and don't reuse this instance after close. /!\\
    Code (in JS here) :
    ...
    let reader = file.reader();
    console.log(writer.readLines()[0]);
    //Outputs the first line of the file to the console
    reader.close();
    """)
public class FileReaderWrapper implements AutoCloseable {
    private final File file;
    private final FileManager.ReadWriteLock lock;
    FileReaderWrapper(@Nonnull File file) {
        this.file = file;
        lock = FileManager.INSTANCE.get(file.getAbsolutePath());
        lock.startRead();
    }

    @Document("""
        Return a list of lines contained in the file.
        """)
    public List<String> readLines() {
        try (var is = new BufferedReader(new FileReader(file))) {
            return is.lines().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Document("""
        Return all the text contained in the file.
        """)
    public String readAllText() {
        return readLines().stream().reduce((acc, str) -> acc + System.lineSeparator() + str).orElse("");
    }

    @Document("""
        Releases read lock for this file.
        """)
    @Override
    public void close() {
        lock.endRead();
    }
}
