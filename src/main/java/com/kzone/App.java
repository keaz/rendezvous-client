package com.kzone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.client.ClientConnector;
import com.kzone.file.FileMetadataMaintainer;
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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello world!
 */
@RequiredArgsConstructor
@Log4j2
public class App {

    public static final String HOST_NAME;
    public static final int PEER_SERVER_PORT = 8018;
    public static final Path DIRECTORY = Paths.get(System.getenv("DIRECTORY") == null ? "/tmp" : System.getenv("DIRECTORY"));
    public static final String METADATA_DIRECTORY = System.getenv("METADATA_DIRECTORY") == null ? ".mtd" : System.getenv("METADATA_DIRECTORY");
    public static final MessageHolder MESSAGE_HOLDER = new MessageHolder();
    static final String HOST = System.getenv("SERVER_HOST") == null ? "host.docker.internal" : System.getenv("SERVER_HOST");
    static final int PORT = 8007;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final JsonDecoder JSON_DECODER = new JsonDecoder(OBJECT_MAPPER);
    public static final JsonEncoder JSON_ENCODER = new JsonEncoder(OBJECT_MAPPER);

    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {

        final var metadataMaintainer = new FileMetadataMaintainer(Paths.get(METADATA_DIRECTORY));
        var folderService = new FolderService(metadataMaintainer);

        final var folderHierarchy = FileUtil.getFolderHierarchy();
        log.debug(folderHierarchy);

        //Creating metadata for folders
        folderHierarchy.forEach(metadataMaintainer::createMetadataDirectoryPath);
        final var fileMetadata = FileUtil.getFileMetadata();
        metadataMaintainer.saveFileMetadata(fileMetadata);

        final var peerClient = new PeerClient(new Bootstrap(), new NioEventLoopGroup(), JSON_DECODER, JSON_ENCODER, new PeerClientHandler(folderService, metadataMaintainer));
        peerClient.init();
        new Thread(new PeerServer(PEER_SERVER_PORT, JSON_DECODER, JSON_ENCODER, new PeerServerHandler(folderService, metadataMaintainer))).start();
        new Thread(new WatchDir(DIRECTORY, FileSystems.getDefault().newWatchService(), metadataMaintainer)).start();
        log.info("Connecting to server {}", HOST);
        new Thread(new ClientConnector(HOST, PORT, new ClientHandler(peerClient))).start();
        new Thread(new Sender(MESSAGE_HOLDER)).start();
    }

}

