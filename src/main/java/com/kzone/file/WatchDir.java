package com.kzone.file;

import com.kzone.App;
import com.kzone.p2p.command.CreateFolderCommand;
import com.kzone.p2p.command.ReadyToUploadCommand;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final FileMetadataMaintainer fileMetadataMaintainer;

    private final Path root;

    /**
     * Creates a WatchService and registers the given directory
     */
    public WatchDir(Path dir, WatchService watcher, FileMetadataMaintainer fileMetadataMaintainer) throws IOException {
        this.watcher = watcher;
        this.root = dir;
        this.fileMetadataMaintainer = fileMetadataMaintainer;
        this.keys = new HashMap<>();

        log.info("Scanning {}", root);
        registerAll(root, false);

        // enable trace after initial registration
        this.trace = true;
    }

    private static void sendCreateFolder(String rootRelativePath) {
        App.MESSAGE_HOLDER.putMessage(new CreateFolderCommand(UUID.randomUUID(), Collections.singletonList(new Folder(rootRelativePath))));
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) {
        try {
            var key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (trace) {
                var prev = keys.get(key);
                if (prev == null) {
                    log.debug("register: {}", dir);
                } else {
                    if (!dir.equals(prev)) {
                        log.debug("update: {} -> {}", prev, dir);
                    }
                }
            }
            keys.put(key, dir);
        } catch (IOException exception) {
            log.error("Failed to register watch {}", dir);
        }
    }

    /**
     * Register the given directory, and all its subdirectories, with the
     * WatchService.
     */
    private void registerAll(final Path start, boolean isNew) throws IOException {
        // register directory and subdirectories
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                if (isNew && Files.isDirectory(dir, NOFOLLOW_LINKS)) {

                    var child = start.resolve(dir);
                    final String rootRelativePath = getRootRelativePath(child);
                    sendCreateFolder(rootRelativePath);
                    sendReadyToUploadCommand(start, child);
                    createMetadata(child,rootRelativePath);

                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void sendReadyToUploadCommand(Path dir, Path child) {
        try (var stream = Files.list(child)) {
            stream.filter(path -> !Files.isDirectory(path)).forEach(path -> {
                final var resolved = child.resolve(path);
                sendUploadCommand(path, root.relativize(root.resolve(resolved)).toString());
            });
        } catch (IOException exception) {
            log.error("Failed to get list of files {}", dir);
        }
    }

    private String getRootRelativePath(Path child) {
        var rootRelative = root.relativize(root.resolve(child));
        return rootRelative.toString();
    }

    private void createMetadata(Path path,String rootRelativePath) {
        final var folderHierarchy = Collections.singletonList(Collections.singletonList(new Folder(rootRelativePath)));
        folderHierarchy.forEach(fileMetadataMaintainer::createMetadataDirectoryPath);
        final var fileMetadata = FileUtil.getFileMetadata(path);
        fileMetadataMaintainer.saveFileMetadata(fileMetadata);
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

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                final var rootRelativePath = rootRelative.toString();
                if (kind == ENTRY_CREATE) {

                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child, true);
//                            sendCreateFolder(rootRelativePath);
                            createMetadata(child,rootRelativePath);
                        } else {
                            sendUploadCommand(child, rootRelativePath);
                        }
                    } catch (IOException x) {
                        log.error("Failed to register ", x);
                    }
                    continue;
                }

                if (kind == ENTRY_MODIFY) {
                    log.info("{} modified ", rootRelative);
                    if (!Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        sendUploadCommand(child, rootRelativePath);
                    }
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

    private void sendUploadCommand(Path path, String rootRelativePath) {
        if (fileMetadataMaintainer.isUpdating(rootRelativePath)) {
            return;
        }
       var checksum = FileUtil.getFileChecksum(path.toAbsolutePath().toFile());
        try {
            if (fileMetadataMaintainer.isModified(rootRelativePath, checksum)) {
                var size = Files.size(path);
                App.MESSAGE_HOLDER.putMessage(new ReadyToUploadCommand(UUID.randomUUID(), rootRelativePath, size, checksum));
                fileMetadataMaintainer.updateMetadata(rootRelativePath, checksum);
            }
        } catch (IOException e) {
            log.error("Failed to read file ", e);
        }
    }
}
