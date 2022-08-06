package com.kzone.file;

import java.io.Serializable;

public record Metadata(String checkSum,boolean idUpdating) implements Serializable {
}
