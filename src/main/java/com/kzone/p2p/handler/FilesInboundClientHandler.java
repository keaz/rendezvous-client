package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.p2p.event.DownloadCompletedEvent;
import com.kzone.util.ClientUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@RequiredArgsConstructor
@Log4j2
public class FilesInboundClientHandler extends ChannelInboundHandlerAdapter {

    private final String fileName;
    private final Long fileSize;
    private final UUID id;
    private final ChannelInboundHandlerAdapter parentAdapter;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object chunkedFile) throws Exception {
        ByteBuf byteBuf = (ByteBuf) chunkedFile;

        final var path = Paths.get(App.DIRECTORY, fileName);
        final var pathTmp = Paths.get("/tmp", fileName);

        if (Files.exists(path)) {
            Files.move(path, pathTmp);
            Files.delete(path);
        }

        Files.createFile(path);
        final var file = path.toFile();
        wrightNewFileContent(file, byteBuf);
        sendResponseOnSuccess(file, ctx);
        Files.delete(pathTmp);
    }

    private void wrightNewFileContent(File file, ByteBuf byteBuf) throws IOException {


        try (var randomAccessFile = new RandomAccessFile(file, "w");
             var channel = randomAccessFile.getChannel()) {
            var lock = channel.tryLock();
            while (byteBuf.isReadable()) {
                randomAccessFile.write(byteBuf.readByte());
            }
            lock.release();
        } catch (final OverlappingFileLockException e) {
            log.error("OverlappingFileLockException ", e);
        }


//        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
//            while (byteBuf.isReadable()) {
//                out.write(byteBuf.readByte());
//            }
//            byteBuf.release();
//        }

    }

    private void sendResponseOnSuccess(File file, ChannelHandlerContext ctx) {
        if (file.length() == fileSize) {
            //After download, we have to set the default pipeline settings
            ctx.pipeline().remove(ChunkedWriteHandler.class);
            ctx.pipeline().remove(FilesInboundClientHandler.class);
            ctx.pipeline().addLast("peer-decoder",App.JSON_DECODER);
            ctx.pipeline().addLast("frameDecoder",
                    new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            ctx.pipeline().addLast(parentAdapter);

            ctx.writeAndFlush(new DownloadCompletedEvent(id, fileName));

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception occurred", cause);
    }
}
