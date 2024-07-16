package com.heig.entities.workflow.file;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileManager {
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

    public synchronized ReadWriteLock get(String absolutePath) {
        return status.computeIfAbsent(absolutePath, key -> new ReadWriteLock());
    }
}
