package com.kzone.p2p.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kzone.p2p.event.PeerEvent;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public record ReadyToUploadCommand(UUID id, String fileName, long fileSize,String checkSum) {
}
