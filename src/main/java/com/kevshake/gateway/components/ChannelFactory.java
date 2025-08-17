package com.kevshake.gateway.components;

import org.jpos.iso.ISOPackager;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.channel.NACChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Factory for creating different types of jPOS channels
 */
@Component
public class ChannelFactory {
    
    @Autowired
    @Qualifier("posPackager")
    private ISOPackager posPackager;
    
    @Autowired
    @Qualifier("bankPackager")
    private ISOPackager bankPackager;
    
    @Autowired
    private BankCommunicationConfig config;
    
    /**
     * Create POS channel (NACC Channel for incoming connections)
     * Note: NACChannel is typically used as server-side channel
     */
    public NACChannel createPosChannel(String host, int port) throws IOException {
        NACChannel channel = new NACChannel();
        channel.setPackager(posPackager);
        return channel;
    }
    
    /**
     * Create Bank channel (ASCII Channel for outgoing connections)
     */
    public ASCIIChannel createBankChannel() throws IOException {
        BankCommunicationConfig.Bank bankConfig = config.getBank();
        ASCIIChannel channel = new ASCIIChannel(bankConfig.getHost(), bankConfig.getPort(), bankPackager);
        channel.setTimeout(bankConfig.getTimeout());
        return channel;
    }
    
    /**
     * Create channel based on configuration type
     */
    public org.jpos.iso.BaseChannel createChannel(String type, String host, int port, ISOPackager packager) throws IOException {
        switch (type.toUpperCase()) {
            case "NACC":
                NACChannel naccChannel = new NACChannel();
                naccChannel.setPackager(packager);
                return naccChannel;
            case "ASCII":
                return new ASCIIChannel(host, port, packager);
            default:
                throw new IllegalArgumentException("Unsupported channel type: " + type);
        }
    }
}
