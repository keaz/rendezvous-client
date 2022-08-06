package com.kzone.p2p.handler;

import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FileUtil;
import com.kzone.file.FolderService;
import com.kzone.p2p.PeersSessionHolder;
import com.kzone.p2p.command.CreateFolderCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
@ChannelHandler.Sharable
public class PeerServerHandler extends PeerHandler {

    static final Queue<Channel> channels = new ConcurrentLinkedQueue<>();


    public PeerServerHandler(FolderService folderService, FileMetadataMaintainer mtdMaintainer) {
        super(folderService, mtdMaintainer);
    }

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
        PeersSessionHolder.getPeersSessionHolder().addPeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort(), channel);

        final var folderHierarchy = FileUtil.getFolderHierarchy();
        folderHierarchy.forEach(folders -> {
            channel.writeAndFlush(new CreateFolderCommand(UUID.randomUUID(), folders));
        });

        channels.add(channel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Closing connection for peer - {}", ctx, cause);

        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.info("Closing connection for peer - {}", ctx);
        final var socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        PeersSessionHolder.getPeersSessionHolder().removePeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        super.channelUnregistered(ctx);
    }
}
