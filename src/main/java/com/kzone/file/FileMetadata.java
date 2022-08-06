package com.kzone.file;

import java.io.Serializable;
import java.util.Map;

public record FileMetadata(String fileName, Metadata metaData) implements Serializable {
}
