package com.kzone.p2p;

import com.kzone.App;
import com.kzone.p2p.event.Message;
import com.kzone.p2p.event.PeerEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
@ChannelHandler.Sharable
public class PeerServerHandler extends SimpleChannelInboundHandler<PeerEvent> {
//    static final List<Channel> channels = new Con<>();
    static final Queue<Channel> channels = new ConcurrentLinkedQueue<>();

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log.debug("Peer joined - {}", ctx);
        final var channel = ctx.channel();
        final var socketAddress = (InetSocketAddress) channel.remoteAddress();
        if (PeersSessionHolder.getPeersSessionHolder().isPeerExists(socketAddress.getAddress().getHostAddress(), socketAddress.getPort())) {
            log.warn("Closing the peer client connection as it is already in the Peer Session {}", socketAddress);
            channel.close();
            return;
        }
        PeersSessionHolder.getPeersSessionHolder().addPeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort(),channel);
        channels.add(channel);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, PeerEvent msg) throws Exception {
        log.info("Message received from peer: {}", msg);

//        for (var c : channels) {
//            if(msg instanceof Message peerMessage) {
//                c.writeAndFlush(new Message(peerMessage.clientId(),peerMessage.message()+" Server UUID :"+ App.uuid));
//            }
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Closing connection for peer - {}", ctx, cause);

        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.info("Closing connection for peer - {}", ctx);
        final var socketAddress =(InetSocketAddress) ctx.channel().remoteAddress();

        PeersSessionHolder.getPeersSessionHolder().removePeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        super.channelUnregistered(ctx);
    }
}
