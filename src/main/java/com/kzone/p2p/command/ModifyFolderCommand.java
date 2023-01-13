package com.kzone.p2p.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kzone.file.Folder;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public record ModifyFolderCommand(String peerHost, UUID id, List<Folder> folders) implements Command {
}
