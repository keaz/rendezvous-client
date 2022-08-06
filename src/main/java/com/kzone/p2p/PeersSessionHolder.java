package com.kzone.p2p;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PeersSessionHolder {

    private final Lock lock = new ReentrantLock();
    private final Condition isEmpty = lock.newCondition();
    private final Condition hasElement = lock.newCondition();

    private static final PeersSessionHolder SINGLETON = new PeersSessionHolder();
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    private PeersSessionHolder() {
    }

    public static PeersSessionHolder getPeersSessionHolder() {
        return SINGLETON;
    }

    public void addPeer(String host, int port, Channel channel) {
        try {
            lock.lock();
            var key = host + port;
            peers.put(key, new Peer(host, port, channel));
            isEmpty.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public void removePeer(String host, int port) {
        try {
            lock.lock();
            var key = host + port;
            peers.remove(key);
            if(peers.isEmpty()){
                hasElement.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public List<Peer> getPeers() {
        try {
            lock.lock();
            while (peers.isEmpty()){
                isEmpty.await();
            }
        }catch (InterruptedException exception){
            Thread.currentThread().interrupt();
        }finally {
            lock.unlock();
        }
        return peers.values().stream().sorted().toList();
    }

    public boolean isPeerExists(String host, int port) {
        return peers.containsKey(host + port);
    }
    public Peer getPeer(String host, int port) {
        return peers.get(host + port);
    }


}
