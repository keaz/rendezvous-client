package com.kzone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import com.kzone.client.ClientConnector;
import com.kzone.file.FileUtil;
import com.kzone.file.FolderService;
import com.kzone.file.WatchDir;
import com.kzone.handler.ClientHandler;
import com.kzone.message.MessageHolder;
import com.kzone.message.Sender;
import com.kzone.p2p.*;
import com.kzone.p2p.handler.PeerClientHandler;
import com.kzone.p2p.handler.PeerServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Hello world!
 */
@RequiredArgsConstructor
@Log4j2
public class App {

    public static final String HOST_NAME;
    public static final int PEER_SERVER_PORT = 8018;
    static final String HOST = System.getenv("SERVER_HOST") == null ? "host.docker.internal" : System.getenv("SERVER_HOST");
    public static final String DIRECTORY = System.getenv("DIRECTORY") == null ? "/tmp" : System.getenv("DIRECTORY");
    static final int PORT = 8007;

    private static final ObjectMapper objectMapper;

    public static final MessageHolder MESSAGE_HOLDER = new MessageHolder();

    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().toString();
            objectMapper =  new ObjectMapper();
            objectMapper.activateDefaultTyping(new DefaultBaseTypeLimitingValidator());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static final JsonDecoder JSON_DECODER = new JsonDecoder(objectMapper);
    public static final JsonEncoder JSON_ENCODER = new JsonEncoder(objectMapper);


    public static void main(String[] args) throws IOException {

        final var s = UUID.randomUUID().toString();
        final var path = Paths.get(App.DIRECTORY, s);
        Files.createDirectory(path);

        var folderService = new FolderService();

        var of = Path.of(DIRECTORY);
        log.info(FileUtil.getFolderHierarchy(of));

        final var peerClient = new PeerClient(new Bootstrap(), new NioEventLoopGroup(), JSON_DECODER, JSON_ENCODER, new PeerClientHandler(folderService));
        peerClient.init();
        new Thread(new PeerServer(PEER_SERVER_PORT, JSON_DECODER, JSON_ENCODER, new PeerServerHandler(folderService))).start();
//        new Thread(new WatchDir(of)).start();
        log.info("Connecting to server {}", HOST);
        new Thread(new ClientConnector(HOST, PORT, new ClientHandler(peerClient))).start();
        new Thread(new Sender(MESSAGE_HOLDER)).start();
    }

}

