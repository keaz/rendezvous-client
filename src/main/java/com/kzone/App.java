package com.kzone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.client.ClientConnector;
import com.kzone.handler.ClientHandler;
import com.kzone.message.MessageHolder;
import com.kzone.message.Sender;
import com.kzone.p2p.*;
import com.kzone.util.CommandLineReader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Hello world!
 */
@RequiredArgsConstructor
@Log4j2
public class App {

    public static final String HOST_NAME;
    public static final int PEER_SERVER_PORT = 8018;
    static final String HOST = System.getenv("SERVER_HOST") == null ? "host.docker.internal" : System.getenv("SERVER_HOST");
    static final int PORT = 8007;

    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        var messageHolder = new MessageHolder();
        var objectMapper = new ObjectMapper();
        final var peerClient = new PeerClient(new Bootstrap(), new NioEventLoopGroup(), new JsonDecoder(objectMapper), new JsonEncoder(objectMapper), new PeerClientHandler());
        peerClient.init();
        new Thread(new PeerServer(PEER_SERVER_PORT, new JsonDecoder(objectMapper), new JsonEncoder(objectMapper), new PeerServerHandler())).start();
        new Thread(new CommandLineReader(messageHolder)).start();
        log.info("Connecting to server {}", HOST);
        new Thread(new ClientConnector(HOST, PORT, new ClientHandler(peerClient))).start();
        new Thread(new Sender(messageHolder)).start();
    }

}

