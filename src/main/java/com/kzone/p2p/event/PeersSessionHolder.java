package com.kzone.p2p.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PeersSessionHolder {

    private static final PeersSessionHolder SINGLETON =  new PeersSessionHolder();

    private static PeersSessionHolder getPeersSessionHolder(){
        return SINGLETON;
    }

    private Map<UUID,Peer> peers = new ConcurrentHashMap<>();

    private PeersSessionHolder(){}

    public void addPeer(ClientJoined notification){
        peers.put(notification.clientId(),
                new Peer(notification.clientId(),notification.clientName(),notification.address()));
    }

    public List<Peer> getPeers(){
        return peers.values().stream().sorted().toList();
    }


}
