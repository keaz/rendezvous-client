package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FileUtil;
import com.kzone.file.FolderService;
import com.kzone.p2p.ChunkedFile;
import com.kzone.p2p.JsonDecoder;
import com.kzone.p2p.JsonEncoder;
import com.kzone.p2p.command.*;
import com.kzone.p2p.event.*;
import com.kzone.util.ClientUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.OverlappingFileLockException;
import java.util.UUID;

@RequiredArgsConstructor
@Log4j2
public class PeerHandler extends ChannelInboundHandlerAdapter {

    protected final FolderService folderService;
    protected final FileMetadataMaintainer mtdMaintainer;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Message received from peer: {}", msg);

        if (msg instanceof CreateFolderCommand command) {
            folderService.createFolder(command.folders());
            return;
        }

        if (msg instanceof ModifyFolderCommand command) {
            folderService.createFolder(command.folders());
            ctx.channel().writeAndFlush(new FolderModifiedEvent(ClientUtil.getClientName(), command.id()));
            return;
        }

        if (msg instanceof ReadyToUploadCommand command) {
            if (mtdMaintainer.isModified(command.fileName(), command.checkSum())) {

                final var path = App.DIRECTORY.resolve(command.fileName());
                var rootRelative = App.DIRECTORY.relativize(path);

                mtdMaintainer.updateMetadata(rootRelative.toString(), command.checkSum());
                ctx.channel().writeAndFlush(new ReadyToReceiveCommand(command.id(), command.fileName()));
                return;
            }

            ctx.channel().writeAndFlush(new UploadRejectedEvent(command.id(), command.fileName()));
            return;
        }


        if (msg instanceof UploadRejectedEvent event) {
            log.info("{} already exists on the peer ", event.fileName());
            return;
        }
        //ready to send
        if (msg instanceof ReadyToReceiveCommand command) {

            var filePath = App.DIRECTORY.resolve(command.fileName());

            final var chunkedFile = new ChunkedFile(new RandomAccessFile(filePath.toFile(), "rwd"), command.fileName(), 102400);
            while (!chunkedFile.isEndOfInput()) {
                final var current = chunkedFile.current();
                final var bytes = chunkedFile.readChunk();
                var id = UUID.randomUUID();
                var addFileChunkCommand = new AddFileChunkCommand(id, chunkedFile.path(), bytes, current);
                final var channelFuture = ctx.channel().writeAndFlush(addFileChunkCommand);
                if (channelFuture.isDone()) {
                    log.debug("Successfully uploaded file {} chunk ID::{}", chunkedFile.path(), id);
                }

            }

            if (chunkedFile.isEndOfInput()) {
                ctx.writeAndFlush(new FileUploadCompletedEvent(UUID.randomUUID(), chunkedFile.path(), FileUtil.getFileChecksum(new File(chunkedFile.path()))));
            }

            return;
        }

        if (msg instanceof AddFileChunkCommand command) {

            var id = command.id();
            final var path = App.DIRECTORY.resolve(command.fileName());
            var rootRelative = App.DIRECTORY.relativize(path);

            try (var randomAccessFile = new RandomAccessFile(path.toFile(), "rwd")) {
                mtdMaintainer.updateStatus(rootRelative.toString(), true);
                randomAccessFile.seek(command.start());
                wrightNewFileContent(randomAccessFile, command.data(), command.start(), command.id());
            } catch (Exception exception) {
                log.error("Failed to create file {} , ID::{} ", path, id, exception);
            }

        }

        if(msg instanceof FileUploadCompletedEvent event){

            final var path = App.DIRECTORY.resolve(event.fileName());
            var rootRelative = App.DIRECTORY.relativize(path);
            mtdMaintainer.updateStatus(rootRelative.toString(), false);
            log.info("Download completed for file {} ID::{}", event.fileName(), event.id());
        }

        if (msg instanceof DownloadCompletedEvent completedEvent) {
            log.info("Client successfully downloaded file {} ID::{}", completedEvent.fileName(), completedEvent.uuid());
        }

        if (msg instanceof DownloadFailedEvent completedEvent) {
            log.info("Client failed to download file {}  ID::{}", completedEvent.fileName(), completedEvent.uuid());
        }

    }

    private void wrightNewFileContent(RandomAccessFile file, byte[] data, int offSet, UUID id) throws IOException {

        try {
            file.write(data, 0, data.length);
        } catch (final OverlappingFileLockException e) {
            log.error("OverlappingFileLockException ID::{}", id, e);
        }

    }

}
