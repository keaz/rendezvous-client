package com.kzone.file;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record FolderMetadata(String filePath, Map<String,FileMetadata> metaData) implements Serializable {
}
