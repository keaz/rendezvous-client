package com.kzone.p2p;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

@Log4j2
public record PeerServer(int port, ChannelInboundHandlerAdapter decoder,
                         ChannelOutboundHandlerAdapter encoder, PeerServerHandler clientHandler) implements Runnable {

    @Override
    public void run() {
        var bossGroup = new NioEventLoopGroup(1);
        var workerGroup = new NioEventLoopGroup();

        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("peer-decoder", decoder);
                    p.addLast("peer-encoder", encoder);
                    p.addLast("peer-handler", clientHandler);
                }
            });
            bootstrap.option(ChannelOption.SO_BACKLOG, 128);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start the server.
            var channel = bootstrap.bind(port).sync();
            log.debug("Peer Server started.");
            // Wait until the server socket is closed.
            channel.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Peer Server thread interrupted ", e);
            Thread.currentThread().interrupt();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
