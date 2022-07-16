package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.p2p.event.PeerEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.Charset;
import java.util.List;

@RequiredArgsConstructor
@ChannelHandler.Sharable
@Log4j2
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final ObjectMapper objectMapper;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
        final var stringMessage = byteBuf.toString(Charset.defaultCharset());
        log.info("******** Message {}", stringMessage);
        final var jsonNode = objectMapper.readTree(stringMessage);
        final Class<?> aClass = Class.forName(jsonNode.get("@type").asText());
        var message = objectMapper.readValue(stringMessage, aClass);
        out.add(message);
    }
}

