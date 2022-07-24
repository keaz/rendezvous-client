package com.kzone.file;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public static List<List<Folder>> getFolderHierarchy(Path root) {
        var folders = new ArrayList<Folder>(0);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (Files.isDirectory(dir) && !Files.isSameFile(root, dir)) {
                        final var resolve = root.resolve(dir);
                        final var relativize = root.relativize(resolve);
                        folders.add(new Folder(relativize.toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var partitionSize = 20;
        var size = folders.size();

        if (size > partitionSize) {
            var fullChunks = (size - 1) / partitionSize;
            return IntStream.range(0, fullChunks + 1).mapToObj(value ->
                    (List<Folder>) new ArrayList<>(folders.subList(value * partitionSize, value == fullChunks ? size : (value + 1) * partitionSize))).toList();
        }

        return Collections.singletonList(folders);
    }

    public static List<FileMetadata> getFileMetadata(Path root) {
        var files = new ArrayList<FileMetadata>(0);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (Files.isDirectory(dir)) {

                        try (var stream = Files.list(dir)) {

                            final var metadataMap = stream.filter(Files::isRegularFile).collect(Collectors.toMap(path -> {
                                final var relativize = dir.relativize(path);
                                return relativize.toString();
                            }, path -> FileUtil.getFileChecksum(path.toFile())));


                            final var resolve = root.resolve(dir);
                            final var relativize = root.relativize(resolve);
                            files.add(new FileMetadata(relativize.toString(), metadataMap));


                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

}
