package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.p2p.event.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;

import java.nio.charset.Charset;
import java.util.List;

@RequiredArgsConstructor
@ChannelHandler.Sharable
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final ObjectMapper objectMapper;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
        final var stringMessage = byteBuf.toString(Charset.defaultCharset());
        Message message = objectMapper.readValue(stringMessage, Message.class);
        out.add(message);
    }
}

