package com.kzone.p2p.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public record DownloadCompletedEvent(String peerHost, UUID uuid, String fileName, String directory) implements PeerEvent{
}
