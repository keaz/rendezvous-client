package com.kzone.p2p.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kzone.p2p.event.PeerEvent;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public record ReadyToUploadCommand(String peerHost, UUID id, String fileName, String directory, long fileSize) implements PeerEvent {
}
