package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FileUtil;
import com.kzone.file.Folder;
import com.kzone.file.FolderService;
import com.kzone.p2p.ChunkedFile;
import com.kzone.p2p.command.*;
import com.kzone.p2p.event.*;
import com.kzone.util.ClientUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Log4j2
public class PeerHandler extends ChannelInboundHandlerAdapter {

    protected final FolderService folderService;
    protected final FileMetadataMaintainer mtdMaintainer;

    private static void handleReadToReceiveCommand(ChannelHandlerContext ctx, UUID id, String fileName) throws Exception {
        var filePath = App.DIRECTORY.resolve(fileName);

        final var chunkedFile = new ChunkedFile(new RandomAccessFile(filePath.toFile(), "rwd"), fileName, 102400);
        while (!chunkedFile.isEndOfInput()) {
            final var current = chunkedFile.current();
            final var bytes = chunkedFile.readChunk();
            var addFileChunkCommand = new AddFileChunkCommand(id, chunkedFile.path(), bytes, current);
            final var channelFuture = ctx.channel().writeAndFlush(addFileChunkCommand);
            if (channelFuture.isDone()) {
                log.debug("Successfully uploaded file {} chunk ID::{}", chunkedFile.path(), id);
            }
        }

        if (chunkedFile.isEndOfInput()) {
            ctx.writeAndFlush(new FileUploadCompletedEvent(UUID.randomUUID(), chunkedFile.path(), FileUtil.getFileChecksum(filePath.toFile().getAbsoluteFile())));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Message received from peer: {}", msg);
        try {
            if (Objects.isNull(msg)) {
                return;
            }

            if (msg instanceof Command command) {
                handleCommand(ctx, command);
                return;
            }
            if (msg instanceof Event event) {
                handleEvent(event);
            }

        } catch (Exception exception) {
            log.error("Exception occurred when handling event {} ", msg, exception);
        }
    }

    private void handleEvent(Event event) {
        switch (event) {
            case UploadRejectedEvent(UUID uuid,String fileName) -> log.info("{} already exists on the peer ", fileName);
            case FileUploadCompletedEvent(UUID id,String fileName,String checkSum) -> handleFileUploadCompletedEvent(fileName);
            case DownloadCompletedEvent(UUID uuid,String fileName) -> log.info("Client successfully downloaded file {} ID::{}", fileName, uuid);
            case DownloadFailedEvent(UUID uuid,String fileName) -> log.info("Client failed to download file {}  ID::{}", fileName, uuid);
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    private void handleFileUploadCompletedEvent(String fileName) {
        final var path = App.DIRECTORY.resolve(fileName);
        var rootRelative = App.DIRECTORY.relativize(path);
        mtdMaintainer.updateStatus(rootRelative.toString(), false);
        log.info("Download completed for file {} ID::{}", fileName, fileName);
    }

    private void handleCommand(ChannelHandlerContext ctx, Command command) throws Exception {
        switch (command) {
            case CreateFolderCommand(UUID id,List<Folder> folders) -> folderService.createFolder(folders);
            case ModifyFolderCommand(String peerHost,UUID id,List<Folder> folders) -> handleModifyCommand(ctx, id, folders);
            case ReadyToUploadCommand(UUID id,String fileName,long fileSize,String checkSum) -> handleReadyToUploadCommand(ctx, id, fileName, checkSum);
            case ReadyToReceiveCommand(UUID id,String fileName) -> handleReadToReceiveCommand(ctx, id, fileName);
            case AddFileChunkCommand(UUID id,String fileName,byte[] data,int start) -> handleAddFileChunkCommand(id, fileName, data, start);
            case default -> throw new IllegalStateException("Unexpected value: " + command);
        }
    }

    private void handleAddFileChunkCommand(UUID id, String fileName, byte[] data, int start) throws IOException {
        final var path = App.DIRECTORY.resolve(fileName);
        var rootRelative = App.DIRECTORY.relativize(path);

        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        try (var randomAccessFile = new RandomAccessFile(path.toFile(), "rwd")) {
            mtdMaintainer.updateStatus(rootRelative.toString(), true);
            randomAccessFile.seek(start);
            wrightNewFileContent(randomAccessFile, data, start, id);
        } catch (Exception exception) {
            log.error("Failed to create file {} , ID::{} ", path, id, exception);
        }
    }

    private void handleReadyToUploadCommand(ChannelHandlerContext ctx, UUID id, String fileName, String checkSum) {
        if (mtdMaintainer.isModified(fileName, checkSum)) {

            final var path = App.DIRECTORY.resolve(fileName);
            var rootRelative = App.DIRECTORY.relativize(path);

            mtdMaintainer.updateMetadata(rootRelative.toString(), checkSum);
            ctx.channel().writeAndFlush(new ReadyToReceiveCommand(id, fileName));
        }

        ctx.channel().writeAndFlush(new UploadRejectedEvent(id, fileName));
    }

    private void handleModifyCommand(ChannelHandlerContext ctx, UUID id, List<Folder> folders) throws IOException {
        folderService.createFolder(folders);
        ctx.channel().writeAndFlush(new FolderModifiedEvent(ClientUtil.getClientName(), id));
    }

    private void wrightNewFileContent(RandomAccessFile file, byte[] data, int offSet, UUID id) throws IOException {

        try {
            file.write(data, 0, data.length);
        } catch (final OverlappingFileLockException e) {
            log.error("OverlappingFileLockException ID::{}", id, e);
        }

    }

}
