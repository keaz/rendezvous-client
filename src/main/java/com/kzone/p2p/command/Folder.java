package com.kzone.p2p.command;

import java.io.Serializable;
import java.util.List;

public record Folder(String folderName,String checksum, List<Folder> childFolders) implements Serializable {
}
