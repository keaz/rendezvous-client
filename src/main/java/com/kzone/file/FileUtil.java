package com.kzone.file;

import com.kzone.p2p.command.Folder;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.io.*;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@UtilityClass
public class FileUtil {

    public static String getFileChecksum(File file) {

        if (file.isDirectory()) {
            return getDigestForDirectory(file);
        }

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

    private String getDigestForDirectory(File file) {
        try (final var outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(outputStream);
             FileInputStream fis = new FileInputStream(file)) {

            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            final var messageDigest = getMessageDigest();
            messageDigest.digest(outputStream.toByteArray());
            return getSHAString(messageDigest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static List<Folder> getFolderHierarchy(Path root) {
        var folders = new ArrayList<Folder>(0);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    log.debug("Visiting directory {}", dir);
                    if (Files.isDirectory(dir)) {
                        final var resolve = root.resolve(dir);
                        final var relativize = root.relativize(resolve);
                        folders.add(new Folder(relativize.toString(),null, Collections.emptyList()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return folders;
    }

}
