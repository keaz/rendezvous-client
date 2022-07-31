package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.p2p.event.FileDownloadedEvent;
import com.kzone.util.ClientUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.UUID;

@RequiredArgsConstructor
@Log4j2
public class FilesInboundHandler extends ChannelInboundHandlerAdapter {

    private final String fileName;
    private final String userDirectory;
    private final Long fileSize;
    private final ChannelInboundHandlerAdapter parentAdapter;
    private final UUID id;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object chunkedFile) throws Exception {
        ByteBuf byteBuf = (ByteBuf) chunkedFile;

        String absoluteFileNameForCloud = userDirectory + "\\" + fileName;

        log.info("Creating file {}", absoluteFileNameForCloud);
        File newfile = new File(absoluteFileNameForCloud);
        final var newFile = newfile.createNewFile();
        log.info("File created {}", newFile);

        wrightNewFileContent(absoluteFileNameForCloud, byteBuf);

        createAnsweraboutSuccessUpload(newfile, ctx);

    }

    private void wrightNewFileContent(String absoluteFileNameForCloud, ByteBuf byteBuf) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(absoluteFileNameForCloud, true))) {
            while (byteBuf.isReadable()) {
                out.write(byteBuf.readByte());
            }
            byteBuf.release();
        }
    }

    private void createAnsweraboutSuccessUpload(File file, ChannelHandlerContext ctx) {
        if (file.length() == fileSize) {
//            ServerPipelineCheckoutService.createBasePipelineAfterUploadForInOutCommandTraffic(ctx);
            ctx.pipeline().remove(ChunkedWriteHandler.class);
//            ctx.pipeline().remove(FilesInboundHandler.class);
            ctx.pipeline().addLast(App.JSON_DECODER);
            ctx.pipeline().addLast(App.JSON_ENCODER);
            ctx.pipeline().addLast(parentAdapter);

            ctx.writeAndFlush(new FileDownloadedEvent(ClientUtil.getClientName(), id));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception occurred", cause);
    }
}
