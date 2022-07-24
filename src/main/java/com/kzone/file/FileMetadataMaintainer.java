package com.kzone.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
@Log4j2
public class FileMetadataMaintainer {

    private static final String MTD_JSON = "mtd.json";
    private final ObjectMapper mapper = new ObjectMapper();

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
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }

                mapper.writeValue(Paths.get(path.toString(), MTD_JSON).toFile(), metadata.metaData());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isModified(String path,String checkSum){
        final var resolve = metaDataDirectory.resolve(path);
        final var parent = resolve.getParent();
        final var file = parent.resolve(MTD_JSON).toFile();
        final var fileName = parent.relativize(resolve).toString();
        try {
            final var mtdJson = mapper.readTree(file);
            if(mtdJson.get(fileName).isNull()){
                return false;
            }
            final var oldChecksum = mtdJson.get(fileName).asText();
            return !checkSum.equalsIgnoreCase(oldChecksum);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
