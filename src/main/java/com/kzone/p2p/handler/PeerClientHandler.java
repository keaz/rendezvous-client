package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FolderService;
import com.kzone.p2p.JsonDecoder;
import com.kzone.p2p.JsonEncoder;
import com.kzone.p2p.command.CreateFolderCommand;
import com.kzone.p2p.command.ModifyFolderCommand;
import com.kzone.p2p.command.ReadyToReceiveCommand;
import com.kzone.p2p.command.ReadyToUploadCommand;
import com.kzone.p2p.event.DownloadCompletedEvent;
import com.kzone.p2p.event.FolderModifiedEvent;
import com.kzone.util.ClientUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class PeerClientHandler extends ChannelInboundHandlerAdapter {

    private final FolderService folderService;
    private final FileMetadataMaintainer mtdMaintainer;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Message from Peer: {}", msg);
        if (msg instanceof CreateFolderCommand command) {
            //TODO create files here
            folderService.createFolder(command.folders());
//            ctx.channel().writeAndFlush(new FolderModifiedEvent(ClientUtil.getClientName(), command.id()));
        }

        if (msg instanceof ModifyFolderCommand command) {
            //TODO create files here
            folderService.createFolder(command.folders());
            ctx.channel().writeAndFlush(new FolderModifiedEvent(ClientUtil.getClientName(), command.id()));
        }

        if (msg instanceof ReadyToUploadCommand command) {
            if(mtdMaintainer.isModified(command.fileName(), command.checkSum())){
                ctx.pipeline().remove(JsonDecoder.class);
                ctx.pipeline().remove(this.getClass());
                ctx.pipeline().remove(LengthFieldBasedFrameDecoder.class);
                ctx.pipeline().addLast(new ChunkedWriteHandler());
                ctx.pipeline().addLast(new FilesInboundClientHandler(command.fileName(), command.fileSize(),  command.id(),this));
                ctx.channel().writeAndFlush(new ReadyToReceiveCommand(command.id(), command.fileName()));
            }

        }

        //ready to send
        if (msg instanceof ReadyToReceiveCommand command) {
            ctx.pipeline().remove(JsonEncoder.class);
            ctx.pipeline().remove(this.getClass());
            ctx.pipeline().remove(LengthFieldPrepender.class);
            ctx.pipeline().addLast(new ChunkedWriteHandler());
//            ctx.pipeline().addLast(new FilesInboundHandler(command.fileName(), command.directory(), command.fileSize(), this, command.id()));

            var filePath = Paths.get(App.DIRECTORY,command.fileName());
            ctx.channel().writeAndFlush(new ChunkedFile(filePath.toFile())).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("Upload success");
                    return;
                }

                if (future.isCancelled()) {
                    log.info("Upload cancelled");
                }
            });
            return;
        }

        if (msg instanceof DownloadCompletedEvent completedEvent) {
            log.info("{} successfully downloaded for id {}", completedEvent.fileName(), completedEvent.uuid());
        }

    }

}