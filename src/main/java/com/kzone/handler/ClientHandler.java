package com.kzone.handler;

import com.kzone.client.event.ClientInfo;
import com.kzone.p2p.PeerClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@RequiredArgsConstructor
@Log4j2
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final PeerClient peerClient;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("Message from Server: {}", msg);
        if (msg instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ClientInfo) {
            peerClient.connect((List<ClientInfo>) list);
        }

    }

}