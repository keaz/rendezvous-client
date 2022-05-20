package com.kzone.p2p;

import io.netty.channel.Channel;

import java.util.Objects;

public record Peer(String host, int port, Channel channel) implements Comparable<Peer>{

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return port == peer.port && host.equals(peer.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public int compareTo(Peer o) {
        return this.host.compareTo(o.host);
    }
}
