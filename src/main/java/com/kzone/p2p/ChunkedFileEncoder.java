package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.file.FileUtil;
import com.kzone.p2p.command.AddFileChunkCommand;
import com.kzone.p2p.event.FileUploadCompletedEvent;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@RequiredArgsConstructor
@Log4j2
@ChannelHandler.Sharable
public class ChunkedFileEncoder extends MessageToMessageEncoder<ChunkedFile> {

    private final ObjectMapper objectMapper;

    @Override
    protected void encode(ChannelHandlerContext ctx, ChunkedFile msg, List<Object> out) throws Exception {
        final var channel = ctx.channel();

        while (msg.isEndOfInput()) {
            final var current = msg.current();
            final var bytes = msg.readChunk();
            var chunkedFile = new AddFileChunkCommand(UUID.randomUUID(), msg.path(), bytes, current);

            final var messageString = objectMapper.writeValueAsString(chunkedFile);

            byte[] messageBytes;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                    gzip.write(messageString.getBytes(StandardCharsets.UTF_8));
                }
                messageBytes = outputStream.toByteArray();

            }

            final var channelFuture = ctx.writeAndFlush(messageBytes);
            if(channelFuture.isDone()){

            }
//            out.add(Unpooled.wrappedBuffer(messageBytes));

        }

        if(msg.isEndOfInput()){
            ctx.writeAndFlush(new FileUploadCompletedEvent(UUID.randomUUID(),msg.path(), FileUtil.getFileChecksum(new File(msg.path()))));
        }

    }

}
