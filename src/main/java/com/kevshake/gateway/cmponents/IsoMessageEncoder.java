package com.kevshake.gateway.cmponents;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

//Encoder: Convert ISOMsg to bytes
public class IsoMessageEncoder extends MessageToByteEncoder<ISOMsg> {
    @SuppressWarnings("unused")
	private final ISOPackager packager;

    public IsoMessageEncoder(ISOPackager packager) {
        this.packager = packager;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ISOMsg msg, ByteBuf out) {
        try {
            byte[] packed = msg.pack();
            out.writeBytes(packed);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}
