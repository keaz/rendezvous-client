package com.kzone.file;

import java.io.Serializable;
import java.util.Map;

public record FileMetadata(String filePath, Map<String,String> metaData) implements Serializable {
}
