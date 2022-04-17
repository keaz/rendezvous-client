package com.kzone.p2p;

import com.kzone.p2p.event.ClientJoined;
import com.kzone.p2p.event.Notification;
import com.kzone.p2p.handler.ClientHandler;
import com.kzone.p2p.message.MessageHolder;
import com.kzone.p2p.message.Sender;
import com.kzone.p2p.util.ClientUtil;
import com.kzone.p2p.util.CommandLineReader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Hello world!
 */
@RequiredArgsConstructor
public class Client implements Runnable{
    static final String HOST = "127.0.0.1";
    static final int PORT = 8007;

    public static void main(String[] args) throws Exception {

        var messageHolder = new MessageHolder();

        final var client = new Client(HOST, PORT,messageHolder);
        new Thread(new CommandLineReader(messageHolder)).start();
        new Thread(client).start();
//        var group = new NioEventLoopGroup();
//        try {
//            var bootstrap = new Bootstrap();
//            bootstrap.group(group) // Set EventLoopGroup to handle all events for client.
//                    .channel(NioSocketChannel.class)// Use NIO to accept new connections.
//                    .option(ChannelOption.SO_KEEPALIVE, true)
//                    .handler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        public void initChannel(SocketChannel ch) throws Exception {
//                            ChannelPipeline p = ch.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
//                            p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Notification.class.getClassLoader())));
//                            p.addLast(new ObjectEncoder());
//
//                            // This is our custom client handler which will have logic for chat.
//                            p.addLast(new ClientHandler());
//
//                        }
//                    });
//
//            // Start the client.
//            var channelFuture = bootstrap.connect(HOST, PORT).sync();
//
//            final var s = UUID.randomUUID();
//            var notification = new ClientJoined(ClientUtil.getClientName(), s, ClientUtil.getHostName());
//            var channel = channelFuture.sync().channel();
//            channel.writeAndFlush(notification);
//            channel.flush();
//
//            // Wait until the connection is closed.
//            channelFuture.channel().closeFuture().sync();
//        } finally {
//            // Shut down the event loop to terminate all threads.
//            group.shutdownGracefully();
//        }
    }

    private final String host;
    private final int port;
    private final MessageHolder messageHolder;

    private Channel channel;



    @Override
    public void run(){
        var group = new NioEventLoopGroup();
        try {
            var bootstrap = new Bootstrap();
            bootstrap.group(group) // Set EventLoopGroup to handle all events for client.
                    .channel(NioSocketChannel.class)// Use NIO to accept new connections.
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                            pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Notification.class.getClassLoader())));
                            pipeline.addLast(new ObjectEncoder());

                            // This is our custom client handler which will have logic for chat.
                            pipeline.addLast(new ClientHandler());

                        }
                    });

            // Start the client.
            var channelFuture = bootstrap.connect(host, port).sync();

            final var s = UUID.randomUUID();
            var notification = new ClientJoined(ClientUtil.getClientName(), s, ClientUtil.getHostName());
            channel = channelFuture.sync().channel();
            channel.writeAndFlush(notification);
            channel.flush();

            new Thread(new Sender(channel,messageHolder)).start();

            // Wait until the connection is closed.
            channelFuture.channel().closeFuture().sync();
        }catch (InterruptedException exception){
            Thread.currentThread().interrupt();
        }
        finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }


}

