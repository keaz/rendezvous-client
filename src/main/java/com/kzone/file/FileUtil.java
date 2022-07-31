package com.kzone.file;

import com.kzone.App;
import com.kzone.p2p.command.ReadyToUploadCommand;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
@UtilityClass
public class FileUtil {

    public static String getFileChecksum(File file) {

        MessageDigest digest = getMessageDigest();
        try (InputStream fis = new FileInputStream(file)) {
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
        } catch (IOException exception) {
            log.error("Failed to create digest for file ", exception);
        }

        return getSHAString(digest);
    }

    private static String getSHAString(MessageDigest digest) {
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<Folder>> getFolderHierarchy() {
        var folders = new ArrayList<Folder>(0);
        try {
            Files.walkFileTree(App.DIRECTORY, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (Files.isDirectory(dir) && !Files.isSameFile(App.DIRECTORY, dir)) {
                        final Path relativize = getRelativize(App.DIRECTORY, dir);
                        folders.add(new Folder(relativize.toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        var fullChunks = getPartitionedList(folders);
        if (fullChunks != null) return fullChunks;

        return Collections.singletonList(folders);
    }


    public static List<ReadyToUploadCommand> getAllFiles() {
        var commands = new ArrayList<ReadyToUploadCommand>(0);
        try {
            Files.walkFileTree(App.DIRECTORY, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (!Files.isDirectory(dir) && !Files.isSymbolicLink(dir)) {
                        final Path relativize = getRelativize(App.DIRECTORY, dir);
                        commands.add(new ReadyToUploadCommand(UUID.randomUUID(),relativize.toString(),Files.size(relativize),FileUtil.getFileChecksum(dir.toFile())));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return commands;
    }

    public static List<List<Folder>> getFolderHierarchy(Path path) {
        var folders = new ArrayList<Folder>(0);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (Files.isDirectory(dir) && !Files.isSameFile(path, dir)) {
                        final Path relativize = getRelativize(App.DIRECTORY, dir);
                        folders.add(new Folder(relativize.toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var fullChunks = getPartitionedList(folders);
        if (fullChunks != null) return fullChunks;

        return Collections.singletonList(folders);
    }



    private static <T> List<List<T>> getPartitionedList(ArrayList<T> folders) {
        //TODO Move this to ENV
        var partitionSize = 20;
        var size = folders.size();


        if (size > partitionSize) {
            var fullChunks = (size - 1) / partitionSize;
            return IntStream.range(0, fullChunks + 1).mapToObj(value ->
                    (List<T>) new ArrayList<>(folders.subList(value * partitionSize, value == fullChunks ? size : (value + 1) * partitionSize))).toList();
        }
        return Collections.emptyList();
    }

    public static List<FolderMetadata> getFileMetadata(Path root) {
        var files = new ArrayList<FolderMetadata>(0);
        try {
            Files.walkFileTree(root, getVisitor(files));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    public static List<FolderMetadata> getFileMetadata() {
        var files = new ArrayList<FolderMetadata>(0);
        try {
            Files.walkFileTree(App.DIRECTORY, getVisitor(files));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    private static SimpleFileVisitor<Path> getVisitor(ArrayList<FolderMetadata> files) {
        return new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.debug("Visiting directory {}", dir);
                if (Files.isDirectory(dir)) {

                    try (var stream = Files.list(dir)) {
                        var metadataMap = getMetadataMap(dir, stream);
                        final Path relativize = getRelativize(App.DIRECTORY, dir);
                        files.add(new FolderMetadata(relativize.toString(), metadataMap));

                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

    private static Map<String, FileMetadata> getMetadataMap(Path dir, Stream<Path> stream) {
        return stream.filter(Files::isRegularFile).map(path -> {
            final var relativize = dir.relativize(path);
            return new FileMetadata(relativize.toString(),new Metadata(FileUtil.getFileChecksum(path.toFile()),false));
        }).collect(Collectors.toMap(FileMetadata::fileName, Function.identity()));
    }

    private static Path getRelativize(Path directory, Path dir) {
        final var resolve = directory.resolve(dir);
        return directory.relativize(resolve);
    }

}
