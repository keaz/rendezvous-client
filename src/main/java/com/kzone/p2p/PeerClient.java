package com.kzone.p2p;

import com.kzone.App;
import com.kzone.client.event.ClientInfo;
import com.kzone.p2p.event.Message;
import com.kzone.util.ClientUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import static com.kzone.App.localPort;
import static com.kzone.App.peerServerStarted;

@Log4j2
public record PeerClient(Bootstrap bootstrap, NioEventLoopGroup group, ChannelInboundHandlerAdapter decoder,
                         ChannelOutboundHandlerAdapter encoder, PeerClientHandler clientHandler) {


    private static final List<ClientInfo> CLIENT_INFO_LIST = new LinkedList<>();
    private static final PeersSessionHolder SESSION_HOLDER = PeersSessionHolder.getPeersSessionHolder();

    public void init() {
        bootstrap.group(group) // Set EventLoopGroup to handle all events for client.
                .channel(NioSocketChannel.class)// Use NIO to accept new connections.
                .option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                pipeline.addLast(decoder);
                pipeline.addLast(encoder);
                pipeline.addLast(clientHandler);

            }
        });
    }

    public void connect(List<ClientInfo> clientInfoList) {

        CLIENT_INFO_LIST.addAll(clientInfoList);

        while(!peerServerStarted){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (ClientInfo clientInfo : clientInfoList) {
//            if(SESSION_HOLDER.isPeerExists(clientInfo.host(),clientInfo.port())){
//                log.warn("Peer already exists with host:{} port:{}",clientInfo.host(),clientInfo.port());
//                continue;
//            }
            try {
                log.info("Connecting to peer: {}", clientInfo);
                final var sync = bootstrap.connect(clientInfo.address(), clientInfo.port()).sync();
                final var channel = sync.channel();
                SESSION_HOLDER.addPeer(clientInfo.address(), clientInfo.port(), channel);
                Message message = new Message(ClientUtil.getMac() + ":" + App.PEER_SERVER_PORT, InetAddress.getLocalHost().toString());
                channel.writeAndFlush(message);
                channel.flush();
            } catch (InterruptedException e) {
                log.error("Error connecting to peer serve", e);
                CLIENT_INFO_LIST.remove(clientInfo);
                Thread.currentThread().interrupt();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
