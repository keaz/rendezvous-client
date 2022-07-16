package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.p2p.event.DownloadCompletedEvent;
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
public class FilesInboundClientHandler extends ChannelInboundHandlerAdapter {

    private final String fileName;
    private final String directory;
    private final Long fileSize;
    private final ChannelInboundHandlerAdapter parentAdapter;

    private final UUID id;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object chunkedFile) throws Exception {
        ByteBuf byteBuf = (ByteBuf) chunkedFile;

        String absoluteFileNameForClient = App.DIRECTORY + File.separator + directory + File.separator + fileName;
        File newfile = new File(absoluteFileNameForClient);

        newfile.createNewFile();

        wrightNewFileContent(absoluteFileNameForClient, byteBuf);

        createAnswerAboutSuccessDownload(newfile, ctx);

    }

    private void wrightNewFileContent(String absoluteFileNameForClient, ByteBuf byteBuf) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(absoluteFileNameForClient, true))) {
            while (byteBuf.isReadable()) {
                out.write(byteBuf.readByte());
            }
            byteBuf.release();
        }
    }

    private void createAnswerAboutSuccessDownload(File file, ChannelHandlerContext ctx) {
        if (file.length() == fileSize) {
            //After download, we have to set the default pipeline settings
            ctx.pipeline().remove(ChunkedWriteHandler.class);
            ctx.pipeline().remove(FilesInboundClientHandler.class);
            ctx.pipeline().addLast(App.JSON_DECODER);
            ctx.pipeline().addLast(parentAdapter);

            ctx.writeAndFlush(new DownloadCompletedEvent(ClientUtil.getClientName(), id, fileName, directory));

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception occurred", cause);
    }
}
