package com.kzone.p2p;

import com.kzone.p2p.handler.PeerServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class PeerServer implements Runnable {
    private final int port;
    private final ChannelInboundHandlerAdapter decoder;
    private final ChannelOutboundHandlerAdapter encoder;
    private final PeerServerHandler clientHandler;

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
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                    pipeline.addLast("frameDecoder",
                            new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                    pipeline.addLast("peer-decoder", decoder);
                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast("peer-encoder", encoder);
                    pipeline.addLast("peer-handler", clientHandler);
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
