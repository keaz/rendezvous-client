package com.kzone.p2p.event;

public record Message(String peerHost, String message) implements PeerEvent {

}
