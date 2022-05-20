package com.kzone.handler;

import com.kzone.client.event.ClientJoined;
import com.kzone.client.event.ClientLeft;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

@Log4j2
public class ResponseDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        ObjectInputStream ois;
        if (byteBuf.hasArray()) {
            ois = new ObjectInputStream(new ByteArrayInputStream(byteBuf.array()));
        } else {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        }
        final var not = (Serializable) ois.readObject();
        log.debug("Got ClientEvent {}", not);
        if (not instanceof List notifications) {

            log.info("App notification {}", notifications);
            list.addAll(notifications);
            return;
        }

        list.add(not);
        if (not instanceof ClientJoined notification) {
            log.info("Got new client join event {}", notification.id());
        }

        if (not instanceof ClientLeft removed) {
            log.info("App removed {}", removed.id());
        }
    }

}
