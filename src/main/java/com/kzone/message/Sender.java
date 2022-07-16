package com.kzone.message;

import com.kzone.p2p.Peer;
import com.kzone.p2p.PeersSessionHolder;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
public class Sender implements Runnable {

    private final PeersSessionHolder peersSessionHolder = PeersSessionHolder.getPeersSessionHolder();
    private final MessageHolder messageHolder;
    private volatile boolean isRunning = true;

    @Override
    public void run() {
        while (isRunning) {
            log.info("Reading messages from  message holder");
            final var peerEvent = messageHolder.readMessage();
            final var peers = peersSessionHolder.getPeers();
            for (Peer peer : peers) {
                final var channel = peer.channel();

                if(!channel.isOpen()){
                    isRunning = false;
                    continue;
                }
                log.info("Sending {} to peer {}",peerEvent,peer.host());
                channel.writeAndFlush(peerEvent);
                channel.flush();
            }

        }
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
