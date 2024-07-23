package com.heig.entities.workflow.file;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File manager (concurrent read / write)
 */
public class FileManager {
    /**
     * Lock for a file
     */
    public static class ReadWriteLock {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final Lock readLock = lock.readLock();
        private final Lock writeLock = lock.writeLock();

        public void startRead() {
            readLock.lock();
        }

        public void endRead() {
            readLock.unlock();
        }

        public void startWrite() {
            writeLock.lock();
        }

        public void endWrite() {
            writeLock.unlock();
        }
    }

    public static FileManager INSTANCE = new FileManager();

    public Map<String, ReadWriteLock> status = new HashMap<>();

    private FileManager() {}

    /**
     * Returns the {@link ReadWriteLock} for the file specified by the absolutePath
     * @param absolutePath The file absolute path
     * @return The {@link ReadWriteLock} for the file
     */
    public synchronized ReadWriteLock get(String absolutePath) {
        return status.computeIfAbsent(absolutePath, key -> new ReadWriteLock());
    }
}
