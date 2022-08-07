package com.kzone.p2p;

import com.kzone.App;
import com.kzone.client.event.ClientInfo;
import com.kzone.file.FileUtil;
import com.kzone.p2p.command.CreateFolderCommand;
import com.kzone.p2p.command.ReadyToUploadCommand;
import com.kzone.p2p.handler.PeerClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

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
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                pipeline.addLast("frameDecoder",
                        new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                pipeline.addLast(decoder);
                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                pipeline.addLast(encoder);
                pipeline.addLast(clientHandler);

            }
        });
    }

    public void connect(List<ClientInfo> clientInfoList) {

        CLIENT_INFO_LIST.addAll(clientInfoList);

        for (ClientInfo clientInfo : clientInfoList) {

            try {
                log.info("Connecting to peer: {}", clientInfo);
                final var sync = bootstrap.connect(clientInfo.address(), clientInfo.port()).sync();
                final var channel = sync.channel();
                SESSION_HOLDER.addPeer(clientInfo.address(), clientInfo.port(), channel);
                final var folderHierarchy = FileUtil.getFolderHierarchy();
                log.debug("******** Peer Sending file hierarchy {}", folderHierarchy);
                folderHierarchy.forEach(folders -> channel.writeAndFlush(new CreateFolderCommand(UUID.randomUUID(), folders)));

//                final var allFiles = FileUtil.getAllFiles();
//                allFiles.forEach(channel::writeAndFlush);
//                channel.writeAndFlush(message);
            } catch (InterruptedException e) {
                log.error("Error connecting to peer serve", e);
                CLIENT_INFO_LIST.remove(clientInfo);
                Thread.currentThread().interrupt();
//            } catch (UnknownHostException e) {
//                throw new RuntimeException(e);
            }
        }
    }

}
