package com.kzone.client;

import com.kzone.App;
import com.kzone.client.event.ClientEvent;
import com.kzone.client.event.ClientJoined;
import com.kzone.handler.ClientHandler;
import com.kzone.util.ClientUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

import static com.kzone.App.localPort;

@Log4j2
public record ClientConnector(String host, int port, ClientHandler clientHandler) implements Runnable {

    @Override
    public void run() {
        var group = new NioEventLoopGroup();
        try {
            var bootstrap = new Bootstrap();
            bootstrap.group(group) // Set EventLoopGroup to handle all events for client.
                    .channel(NioSocketChannel.class)// Use NIO to accept new connections.
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                            pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(ClientEvent.class.getClassLoader())));
                            pipeline.addLast(new ObjectEncoder());

                            // This is our custom client handler which will have logic for chat.
                            pipeline.addLast(clientHandler);

                        }
                    });

            // Start the client.
            var channelFuture = bootstrap.connect(host, port).sync();

            var notification = new ClientJoined(ClientUtil.getMac() + ":" + App.PEER_SERVER_PORT, App.PEER_SERVER_PORT);
            Channel channel = channelFuture.sync().channel();
            channel.writeAndFlush(notification);
            channel.flush();
            log.info("Client connector remote address {}" ,channel.remoteAddress());
            log.info("Client connector remote address {}" ,channel.localAddress());
            localPort = ((InetSocketAddress)channel.localAddress()).getPort();
            // Wait until the connection is closed.
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}
