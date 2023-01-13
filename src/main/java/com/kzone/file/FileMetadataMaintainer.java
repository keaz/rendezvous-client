package com.kzone.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Log4j2
public class FileMetadataMaintainer {

    private static final String MTD_JSON = "mtd.json";
    private final ObjectMapper mapper = new ObjectMapper();

    private final Path metaDataDirectory;


    public void createMetadataDirectoryPath(List<Folder> folders) {
        folders.forEach(this::create);
    }

    private void create(Folder folder) {
        try {
            final var resolve = metaDataDirectory.resolve(folder.folderName());
            if (Files.exists(resolve)) {
                return;
            }
            Files.createDirectories(metaDataDirectory.resolve(folder.folderName()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveFileMetadata(List<FolderMetadata> folderMetadata) {
        folderMetadata.forEach(this::updateMetadata);
    }

    private void updateMetadata(FolderMetadata metadata) {
        final var path = Paths.get(metaDataDirectory.toAbsolutePath().toString(), metadata.filePath());

        try {
            Files.createDirectories(path);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            mapper.writeValue(Paths.get(path.toString(), MTD_JSON).toFile(), metadata);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isModified(String path, String checkSum) {
        final var resolve = metaDataDirectory.resolve(path);
        final var parent = resolve.getParent();
        final var fileName = parent.relativize(resolve).toString();
        final var file = parent.resolve(MTD_JSON);
        if (!Files.exists(file)) {
            return true;
        }
        try {
            final var folderMetadata = mapper.readValue(file.toFile(), FolderMetadata.class);
            final var fileMetadataMap = folderMetadata.metaData();
            if (!fileMetadataMap.containsKey(fileName)) {
                return true;
            }

            final var fileMetadata = fileMetadataMap.get(fileName);
            final var oldChecksum = fileMetadata.metaData().checkSum();
            return !checkSum.equalsIgnoreCase(oldChecksum);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateMetadata(String path, String newCheckSum) {
        final var resolve = metaDataDirectory.resolve(path);
        final var parent = resolve.getParent();
        final var fileName = parent.relativize(resolve).toString();
        final var mtdJsonPath = parent.resolve(MTD_JSON);
        final var file = mtdJsonPath.toFile();
        try {

            Files.createDirectories(parent);
            if (!Files.exists(mtdJsonPath)) {
                var metaData = Map.of(fileName, new FileMetadata(fileName, new Metadata(newCheckSum, false)));
                mapper.writeValue(mtdJsonPath.toFile(), new FolderMetadata(fileName, metaData));

                return;
            }

            final var folderMetadata = mapper.readValue(file, FolderMetadata.class);
            final var fileMetadataMap = folderMetadata.metaData();
            final var fileMetadata = new FileMetadata(fileName, new Metadata(newCheckSum, false));
            fileMetadataMap.put(fileName, fileMetadata);

            final var bytes = mapper.writer().writeValueAsBytes(folderMetadata);
            Files.write(mtdJsonPath, bytes, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateStatus(String path, boolean isUpdating) {
        final var resolve = metaDataDirectory.resolve(path);
        final var parent = resolve.getParent();
        final var fileName = parent.relativize(resolve).toString();
        final var mtdJsonPath = parent.resolve(MTD_JSON);
        final var file = mtdJsonPath.toFile();
        try {
            final var folderMetadata = mapper.readValue(file, FolderMetadata.class);
            final var fileMetadataMap = folderMetadata.metaData();
            final var oldMetadata = fileMetadataMap.get(fileName);
            final var fileMetadata = new FileMetadata(fileName, new Metadata(oldMetadata.metaData().checkSum(), isUpdating));
            fileMetadataMap.put(fileName, fileMetadata);

            final var bytes = mapper.writer().writeValueAsBytes(folderMetadata);
            Files.write(mtdJsonPath, bytes, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isUpdating(String path) {
        final var resolve = metaDataDirectory.resolve(path);
        final var parent = resolve.getParent();
        final var fileName = parent.relativize(resolve).toString();
        final var mtdJsonPath = parent.resolve(MTD_JSON);
        if (!Files.exists(mtdJsonPath)) {
            return false;
        }
        final var file = mtdJsonPath.toFile();
        try {
            final var folderMetadata = mapper.readValue(file, FolderMetadata.class);
            final var fileMetadataMap = folderMetadata.metaData();
            final var oldMetadata = fileMetadataMap.get(fileName);
            if (oldMetadata == null) {
                return false;
            }
            return oldMetadata.metaData().idUpdating();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
