package com.kzone.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzone.client.event.ClientEvent;
import com.kzone.p2p.event.Message;
import com.kzone.p2p.event.PeerEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

@RequiredArgsConstructor
@Log4j2
@ChannelHandler.Sharable
public class JsonEncoder extends MessageToMessageEncoder<Object> {

    private final ObjectMapper objectMapper;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        final var messageString = objectMapper.writeValueAsString(msg);

        out.add(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(messageString), Charset.defaultCharset()));


    }

}
