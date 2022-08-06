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
import com.kzone.p2p.event.DownloadFailedEvent;
import com.kzone.p2p.event.FolderModifiedEvent;
import com.kzone.p2p.event.UploadRejectedEvent;
import com.kzone.util.ClientUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class PeerHandler extends ChannelInboundHandlerAdapter {

    protected final FolderService folderService;
    protected final FileMetadataMaintainer mtdMaintainer;


    private ChunkedWriteHandler removeFileEncoder(ChannelHandlerContext ctx) {
        return ctx.pipeline().remove(ChunkedWriteHandler.class);
    }

    private void addJsonEncoders(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(App.JSON_ENCODER);
        ctx.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
    }

    private void addFileEncoders(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new ChunkedWriteHandler());
    }

    private void removeJsonEncoders(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(JsonEncoder.class);
        ctx.pipeline().remove(LengthFieldPrepender.class);
    }

    private void addFileDecoders(ChannelHandlerContext ctx, ReadyToUploadCommand command) {
        addFileEncoders(ctx);
        ctx.pipeline().addLast(new FilesInboundClientHandler(command.fileName(), command.fileSize(),  command.id(),command.checkSum(),this,mtdMaintainer));
    }

    private void removedJsonDecoders(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(JsonDecoder.class);
        ctx.pipeline().remove(this.getClass());
        ctx.pipeline().remove(LengthFieldBasedFrameDecoder.class);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("Message received from peer: {}", msg);

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
            if(mtdMaintainer.isModified(command.fileName(), command.checkSum())){
                removedJsonDecoders(ctx);
                addFileDecoders(ctx, command);
                ctx.channel().writeAndFlush(new ReadyToReceiveCommand(command.id(), command.fileName()));
                return;
            }

            ctx.channel().writeAndFlush(new UploadRejectedEvent(command.id(), command.fileName()));
            return;
        }

        if(msg instanceof UploadRejectedEvent event){
            log.info("{} already exists on the peer ",event.fileName());
            return;
        }
        //ready to send
        if (msg instanceof ReadyToReceiveCommand command) {
            removeJsonEncoders(ctx);
            addFileEncoders(ctx);

            var filePath = App.DIRECTORY.resolve(command.fileName());
            ctx.channel().writeAndFlush(new ChunkedFile(filePath.toFile(),80000000)).addListener((ChannelFutureListener) future -> {
                addJsonEncoders(ctx);
                removeFileEncoder(ctx);
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
            log.info("Client successfully downloaded file {} ID::{}", completedEvent.fileName(), completedEvent.uuid());
        }

        if (msg instanceof DownloadFailedEvent completedEvent) {
            log.info("Client failed to download file {}  ID::{}", completedEvent.fileName(), completedEvent.uuid());
        }

    }

}
