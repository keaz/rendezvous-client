package com.kzone.file;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

@Log4j2
public class WatchDir implements Runnable {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean trace;

    private final Path root;

    /**
     * Creates a WatchService and registers the given directory
     */
    public WatchDir(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.root = dir;

        log.info("Scanning {}", root);
        registerAll(root);

        // enable trace after initial registration
        this.trace = true;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }


    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        var key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            var prev = keys.get(key);
            if (prev == null) {
                log.info("register: {}", dir);
            } else {
                if (!dir.equals(prev)) {
                    log.info("update: {} -> {}", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its subdirectories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and subdirectories
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void run() {
        for (; ; ) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                return;
            }

            var dir = keys.get(key);
            if (dir == null) {
                log.warn("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                var kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                var ev = (WatchEvent<Path>) event;
                var name = ev.context();
                var child = dir.resolve(name);
                var rootRelative = root.relativize(root.resolve(child));

                // print out event
                log.info("{}: {}", event.kind().name(), rootRelative);
                log.info("Checksum {} for {} ", FileUtil.getFileChecksum(rootRelative.toFile()), rootRelative);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        log.error("Failed to register ", x);
                    }
                    continue;
                }

                if (kind == ENTRY_MODIFY) {
                    log.info("{} modified ", rootRelative);

                    continue;
                }
                if (kind == ENTRY_DELETE) {
                    log.info("{} deleted ", rootRelative);

                }
            }

            // reset key and remove from set if directory no longer accessible
            var valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
}
