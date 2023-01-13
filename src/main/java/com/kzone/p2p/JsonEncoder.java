package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@RequiredArgsConstructor
@Log4j2
@ChannelHandler.Sharable
public class JsonEncoder extends MessageToMessageEncoder<Object> {

    private final ObjectMapper objectMapper;

    private static byte[] gZipMessage(String messageString) throws IOException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                gzip.write(messageString.getBytes(StandardCharsets.UTF_8));
            }
            return outputStream.toByteArray();
        }

    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

        final var messageString = objectMapper.writeValueAsString(msg);

        byte[] messageBytes = gZipMessage(messageString);
        out.add(Unpooled.wrappedBuffer(messageBytes));

    }

}
