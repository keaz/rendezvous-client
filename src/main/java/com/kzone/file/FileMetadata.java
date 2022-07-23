package com.kzone.file;

import java.io.Serializable;

public record FileMetadata(String filePath, String checksum) implements Serializable {
}
