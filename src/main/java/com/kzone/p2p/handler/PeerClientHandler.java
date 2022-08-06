package com.kzone.p2p.handler;

import com.kzone.App;
import com.kzone.file.FileMetadataMaintainer;
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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Log4j2
@ChannelHandler.Sharable
public class PeerClientHandler extends PeerHandler {

    public PeerClientHandler(FolderService folderService,FileMetadataMaintainer mtdMaintainer) {
        super(folderService,mtdMaintainer);
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