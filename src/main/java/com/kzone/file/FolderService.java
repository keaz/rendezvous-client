package com.kzone.file;

import com.kzone.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
public class FolderService {

    private final FileMetadataMaintainer metadataMaintainer;

    public void createFolder(List<Folder> folders) throws IOException {

        for (Folder folder : folders) {
            var folderPath = App.DIRECTORY + File.separator + folder.folderName();
            log.info("Creating folder {}", folderPath);
            final var path = Paths.get(folderPath);
            Files.createDirectories(path);
        }
        //TODO remove this, JUST for testing.
        final var folderHierarchy = FileUtil.getFolderHierarchy();
        folderHierarchy.forEach(metadataMaintainer::createMetadataDirectoryPath);
        final var fileMetadata = FileUtil.getFileMetadata();
        metadataMaintainer.saveFileMetadata(fileMetadata);
    }

}
