package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FileUtil;
import com.kzone.p2p.event.DownloadCompletedEvent;
import com.kzone.p2p.event.DownloadFailedEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Log4j2
public class FilesInboundClientHandler extends ChannelInboundHandlerAdapter {

    private final String fileName;
    private final Long fileSize;
    private final UUID id;
    private final ChannelInboundHandlerAdapter parentAdapter;
    private final FileMetadataMaintainer mtdMaintainer;
    private final RandomAccessFile randomAccessFile;
    private final FileLock lock;
    private final Path rootRelative;
    private final String checkSum;

    public FilesInboundClientHandler(String fileName, Long fileSize, UUID id,String checkSum, ChannelInboundHandlerAdapter parentAdapter, FileMetadataMaintainer mtdMaintainer) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.id = id;
        this.parentAdapter = parentAdapter;
        this.mtdMaintainer = mtdMaintainer;
        this.checkSum = checkSum;

        try {
            final var path = App.DIRECTORY.resolve(fileName);
            this.rootRelative = App.DIRECTORY.relativize(path);
            mtdMaintainer.updateMetadata(this.rootRelative.toString(),this.checkSum);
            mtdMaintainer.updateStatus(this.rootRelative.toString(),true);
            final var pathTmp = Paths.get("/tmp", fileName);

//            if (Files.exists(path) && Files.isRegularFile(path)) {
//                log.debug("Moving existing file to temp {}", path);
//                Files.move(path, pathTmp);
//            }
//            Files.createFile(path);
            this.randomAccessFile = new RandomAccessFile(path.toFile(), "rwd");
            var fileChannel = randomAccessFile.getChannel();
            this.lock = fileChannel.tryLock();

        } catch (IOException exception) {
            log.error("Failed ot move {} to temporary location ", fileName, exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object chunkedFile) throws Exception {
        ByteBuf byteBuf = (ByteBuf) chunkedFile;

        final var path = App.DIRECTORY.resolve(fileName);
        final var pathTmp = Paths.get("/tmp", fileName);

        try {

            final var file = path.toFile();
            wrightNewFileContent(file, byteBuf);
            sendResponseOnSuccess(path, ctx);

//            if (Files.exists(pathTmp)) {
//                log.debug("Deleting temp file {} , ID::{}", pathTmp, id);
//                Files.delete(pathTmp);
//            }
        } catch (IOException exception) {
            sendResponseOnFailure(ctx);
            log.error("Failed to create file {} , ID::{} ", path, id, exception);
        }
    }

    private void sendResponseOnFailure(ChannelHandlerContext ctx) throws IOException {
        try {
            ctx.pipeline().remove(ChunkedWriteHandler.class);
            ctx.pipeline().remove(this.getClass());
            ctx.pipeline().addLast("peer-decoder", App.JSON_DECODER);
            ctx.pipeline().addLast("frameDecoder",
                    new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            ctx.pipeline().addLast(parentAdapter);
            log.debug("Sending Failed to download event for file {} , ID::{}", id, fileName);
            ctx.writeAndFlush(new DownloadFailedEvent(id, fileName));
        } finally {
            lock.release();
        }
    }

    private void wrightNewFileContent(File file, ByteBuf byteBuf) throws IOException {

        try {
            while (byteBuf.isReadable()) {
                randomAccessFile.write(byteBuf.readByte());
            }
        } catch (final OverlappingFileLockException e) {
            log.error("OverlappingFileLockException ID::{}", id, e);
        }

    }

    private void sendResponseOnSuccess(Path path, ChannelHandlerContext ctx) throws IOException {

        if (Files.size(path) == fileSize) {
            try {
                final var parent = path.getParent();
                final var folderHierarchy = FileUtil.getFolderHierarchy(parent);
                folderHierarchy.forEach(mtdMaintainer::createMetadataDirectoryPath);
                final var fileMetadata = FileUtil.getFileMetadata(parent);

                mtdMaintainer.saveFileMetadata(fileMetadata);
                mtdMaintainer.updateStatus(rootRelative.toString(),false);

                //After download, we have to set the default pipeline settings
                ctx.pipeline().remove(ChunkedWriteHandler.class);
                ctx.pipeline().remove(this.getClass());
                ctx.pipeline().addLast("peer-decoder", App.JSON_DECODER);
                ctx.pipeline().addLast("frameDecoder",
                        new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
                ctx.pipeline().addLast(parentAdapter);
                log.debug("Sending Download completed event for file {} , ID::{}", id, fileName);
                ctx.writeAndFlush(new DownloadCompletedEvent(id, fileName));
            } finally {
                lock.release();
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception occurred ID::{}", id, cause);
    }
}
