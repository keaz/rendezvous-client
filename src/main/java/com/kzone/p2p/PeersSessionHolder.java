package com.kzone.p2p;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeersSessionHolder {

    private static final PeersSessionHolder SINGLETON = new PeersSessionHolder();
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    private PeersSessionHolder() {
    }

    public static PeersSessionHolder getPeersSessionHolder() {
        return SINGLETON;
    }

    public void addPeer(String host, int port, Channel channel) {
        var key = host + port;
        peers.put(key, new Peer(host, port, channel));
    }

    public void removePeer(String host, int port) {
        var key = host + port;
        peers.remove(key);
    }

    public List<Peer> getPeers() {
        return peers.values().stream().sorted().toList();
    }

    public boolean isPeerExists(String host, int port) {
        return peers.containsKey(host + port);
    }
    public Peer getPeer(String host, int port) {
        return peers.get(host + port);
    }


}
