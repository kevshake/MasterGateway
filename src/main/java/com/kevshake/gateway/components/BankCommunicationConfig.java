package com.kevshake.gateway.components;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for bank communication settings
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "iso8583")
public class BankCommunicationConfig {
    
    private Pos pos = new Pos();
    private Bank bank = new Bank();
    
    public static class Pos {
        private int port = 5878;
        private String channelType = "NACC";
        private String packager = "org.jpos.iso.packager.ISO87APackager";
        private boolean nettyServerEnabled = true;
        
        /** TCP listen port for inbound POS ISO8583 (Netty). */
        public int getPort() { return port; }
        /** Sets the port. */
        public void setPort(int port) { this.port = port; }
        /** jPOS channel type for POS sessions (e.g. NACC). */
        public String getChannelType() { return channelType; }
        /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
        /** Fully qualified jPOS packager class for POS messages. */
        public String getPackager() { return packager; }
        /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }

        /** Returns whether the embedded Netty POS ISO8583 server should start. */
        public boolean isNettyServerEnabled() { return nettyServerEnabled; }

        /** Sets whether the embedded Netty POS ISO8583 server should start. */
        public void setNettyServerEnabled(boolean nettyServerEnabled) { this.nettyServerEnabled = nettyServerEnabled; }
    }
    
    public static class Bank {
        private String host;
        private int port = 8001;
        private String channelType = "ASCII";
        private String packager = "org.jpos.iso.packager.ISO87BPackager";
        private int timeout = 30000;
        private int maxConnections = 5;
        private Retry retry = new Retry();
        
        public static class Retry {
            private int maxAttempts = 3;
            private int delayMs = 5000;
            private double backoffMultiplier = 2.0;
            
            /** Maximum outbound bank reconnect attempts per transaction burst. */
            public int getMaxAttempts() { return maxAttempts; }
            /** Sets the max attempts. */
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            /** Initial delay in milliseconds before the first bank retry. */
            public int getDelayMs() { return delayMs; }
            /** Sets the delay ms. */
            public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
            /** Exponential backoff factor applied between bank retries. */
            public double getBackoffMultiplier() { return backoffMultiplier; }
            /** Sets the backoff multiplier. */
            public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        }
        
        /** Bank/switch hostname or IP (bound from {@code ISO8583_BANK_HOST}). */
        public String getHost() { return host; }
        /** Sets the host. */
        public void setHost(String host) { this.host = host; }
        /** TCP port on the bank/switch endpoint. */
        public int getPort() { return port; }
        /** Sets the port. */
        public void setPort(int port) { this.port = port; }
        /** jPOS channel type for bank sessions. */
        public String getChannelType() { return channelType; }
        /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
        /** Fully qualified jPOS packager class for bank messages. */
        public String getPackager() { return packager; }
        /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }
        /** Socket read/connect timeout in milliseconds for bank I/O. */
        public int getTimeout() { return timeout; }
        /** Sets the timeout. */
        public void setTimeout(int timeout) { this.timeout = timeout; }
        /** Upper bound on concurrent bank channel connections. */
        public int getMaxConnections() { return maxConnections; }
        /** Sets the max connections. */
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        /** Nested retry policy for bank reconnects. */
        public Retry getRetry() { return retry; }
        /** Sets the retry. */
        public void setRetry(Retry retry) { this.retry = retry; }
    }
    
    /** POS-side ISO8583 listener settings under {@code iso8583.pos}. */
    public Pos getPos() { return pos; }
    /** Sets the pos. */
    public void setPos(Pos pos) { this.pos = pos; }
    /** Bank/switch outbound channel settings under {@code iso8583.bank}. */
    public Bank getBank() { return bank; }
    /** Sets the bank. */
    public void setBank(Bank bank) { this.bank = bank; }
}
