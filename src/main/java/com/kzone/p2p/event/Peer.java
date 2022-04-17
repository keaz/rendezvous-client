package com.kzone.p2p.event;

import java.util.UUID;

public record Peer(UUID peerId, String name, String address) implements Comparable<Peer> {

    @Override
    public int compareTo(Peer otherPeer) {
        return name.compareTo(otherPeer.name);
    }
}
