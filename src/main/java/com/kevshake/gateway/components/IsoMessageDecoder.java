package com.kevshake.gateway.components;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import java.util.List;

//Decoder: Convert bytes to ISOMsg
public class IsoMessageDecoder extends ByteToMessageDecoder {
    @SuppressWarnings("unused")
	private final ISOPackager packager;

    public IsoMessageDecoder(ISOPackager packager) {
        this.packager = packager;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 2) {
            return;
        }
        
        byte[] buffer = new byte[in.readableBytes()];
        in.readBytes(buffer);
        
        try {
            ISOMsg msg = new ISOMsg();
            msg.unpack(buffer);
            out.add(msg);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}