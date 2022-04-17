package com.kzone.p2p.message;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class Sender implements Runnable {

    private final Channel channel;
    private final MessageHolder messageHolder;
    private volatile boolean isRunning = true;

    @Override
    public void run() {
        while (isRunning) {
            if(!channel.isOpen()){
                isRunning = false;
                break;
            }
            final var notification = messageHolder.readMessage();
            channel.writeAndFlush(notification);
            channel.flush();
        }
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
