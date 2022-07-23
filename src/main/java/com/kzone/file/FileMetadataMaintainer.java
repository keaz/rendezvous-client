package com.kzone.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
@Log4j2
public class FileMetadataMaintainer {

    private final Path metaDataDirectory;


    public void createMetadataDirectoryPath(List<Folder> folders) {
        folders.forEach(folder -> {
            try {
                final var resolve = metaDataDirectory.resolve(folder.folderName());
                if (Files.exists(resolve)) {
                    return;
                }
                Files.createDirectories(metaDataDirectory.resolve(folder.folderName()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void saveFileMetadata(List<FileMetadata> fileMetadata) {
        fileMetadata.forEach(metadata -> {
            final var path = Paths.get(metaDataDirectory.toAbsolutePath().toString(), metadata.filePath());
            try {
                final var resolve = path.getParent().resolve(metadata.checksum());
                if (Files.exists(resolve)) {
                    Files.delete(resolve);
                }
                Files.createFile(resolve);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
