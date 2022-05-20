package com.kzone.handler;

import com.kzone.client.event.ClientJoined;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

@Log4j2
public class RequestEncoder extends MessageToByteEncoder<ClientJoined> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ClientJoined notification, ByteBuf byteBuf) throws Exception {
        try (var outputStream = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(outputStream)) {

            oos.writeObject(notification);
            final var bytes = outputStream.toByteArray();
            ByteBuffer length = ByteBuffer.allocate(4);
            length.putInt(bytes.length);

            byteBuf.writeBytes(length);
            byteBuf.writeBytes(bytes);
        } catch (IOException exception) {
            log.error("Failed to encode message {}", notification, exception);
        }
    }

}
