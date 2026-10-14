package com.kevshake.gateway.components;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

//Encoder: Convert ISOMsg to bytes
public class IsoMessageEncoder extends MessageToByteEncoder<ISOMsg> {
    @SuppressWarnings("unused")
	private final ISOPackager packager;
    /** Binds the POS ISO packager used when serializing outbound messages. */
    public IsoMessageEncoder(ISOPackager packager) {
        this.packager = packager;
    }

    /** Encodes a jPOS {@link ISOMsg} to bytes using the POS packager. */
    @Override
    /** Implements encode for IsoMessageEncoder. */
    protected void encode(ChannelHandlerContext ctx, ISOMsg msg, ByteBuf out) {
        try {
            byte[] packed = packager.pack(msg);
            out.writeBytes(packed);
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}
