package com.kzone.file;

import com.kzone.App;
import com.kzone.p2p.command.Folder;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
public class FolderService {

    public void createFolder(List<Folder> folders) throws IOException {

        for (Folder folder : folders) {
            var folderPath = App.DIRECTORY + File.separator + folder.folderName();
            log.info("Creating folder {}",folderPath);
            final var path = Paths.get(folderPath);
            Files.createDirectories(path);
            createFolder(folder.childFolders());
        }
    }

}
