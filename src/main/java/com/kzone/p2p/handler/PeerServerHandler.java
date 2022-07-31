package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
import com.kzone.file.FileUtil;
import com.kzone.file.FolderService;
import com.kzone.p2p.JsonDecoder;
import com.kzone.p2p.JsonEncoder;
import com.kzone.p2p.PeersSessionHolder;
import com.kzone.p2p.command.CreateFolderCommand;
import com.kzone.p2p.command.ModifyFolderCommand;
import com.kzone.p2p.command.ReadyToReceiveCommand;
import com.kzone.p2p.command.ReadyToUploadCommand;
import com.kzone.p2p.event.DownloadCompletedEvent;
import com.kzone.p2p.event.DownloadFailedEvent;
import com.kzone.p2p.event.FolderModifiedEvent;
import com.kzone.p2p.event.UploadRejectedEvent;
import com.kzone.util.ClientUtil;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class PeerServerHandler extends ChannelInboundHandlerAdapter {
    //    static final List<Channel> channels = new Con<>();
    static final Queue<Channel> channels = new ConcurrentLinkedQueue<>();
    private final FolderService folderService;
    private final FileMetadataMaintainer mtdMaintainer;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log.debug("Peer joined - {}", ctx);
        final var channel = ctx.channel();
        final var socketAddress = (InetSocketAddress) channel.remoteAddress();
        if (PeersSessionHolder.getPeersSessionHolder().isPeerExists(socketAddress.getAddress().getHostAddress(), socketAddress.getPort())) {
            log.warn("Closing the peer client connection as it is already in the Peer Session {}", socketAddress);
            channel.close();
            return;
        }
        PeersSessionHolder.getPeersSessionHolder().addPeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort(), channel);

        final var folderHierarchy = FileUtil.getFolderHierarchy();
        folderHierarchy.forEach(folders -> {
            channel.writeAndFlush(new CreateFolderCommand(UUID.randomUUID(), folders));
        });

        channels.add(channel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("Message received from peer: {}", msg);

        if (msg instanceof CreateFolderCommand command) {
            //TODO create files here
            folderService.createFolder(command.folders());
            return;
//            ctx.channel().writeAndFlush(new FolderModifiedEvent(ClientUtil.getClientName(), command.id()));
        }

        if (msg instanceof ModifyFolderCommand command) {
            //TODO create files here
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
//            ctx.pipeline().addLast(new FilesInboundHandler(command.fileName(), command.directory(), command.fileSize(), this, command.id()));

            var filePath = App.DIRECTORY.resolve(command.fileName());
            ctx.channel().writeAndFlush(new ChunkedFile(filePath.toFile())).addListener((ChannelFutureListener) future -> {
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
            log.info("{} successfully downloaded for id {}", completedEvent.fileName(), completedEvent.uuid());
            return;
        }

        if (msg instanceof DownloadFailedEvent completedEvent) {
            log.info("{} failed to downloaded for id {}", completedEvent.fileName(), completedEvent.uuid());
            return;
        }
//        for (var c : channels) {
//            if(msg instanceof Message peerMessage) {
//                c.writeAndFlush(new Message(peerMessage.clientId(),peerMessage.message()+" Server UUID :"+ App.uuid));
//            }
//        }
    }

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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Closing connection for peer - {}", ctx, cause);

        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.info("Closing connection for peer - {}", ctx);
        final var socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

        PeersSessionHolder.getPeersSessionHolder().removePeer(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
        super.channelUnregistered(ctx);
    }
}
