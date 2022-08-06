package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

@RequiredArgsConstructor
@ChannelHandler.Sharable
@Log4j2
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final ObjectMapper objectMapper;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {

        final var bytes = ByteBufUtil.getBytes(byteBuf);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            int b;
            while ((b = gis.read()) != -1) {
                baos.write((byte) b);
            }
        }

        final var stringMessage = baos.toString(StandardCharsets.UTF_8);
        log.debug("Received message {}",stringMessage);
        final var jsonNode = objectMapper.readTree(stringMessage);
        final Class<?> aClass = Class.forName(jsonNode.get("@type").asText());
        var message = objectMapper.readValue(stringMessage, aClass);
        out.add(message);
    }
}

