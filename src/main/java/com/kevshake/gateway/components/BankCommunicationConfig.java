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
        
        // Getters and setters
        public int getPort() { return port; }
    /** Sets the port. */
        public void setPort(int port) { this.port = port; }
    public String getChannelType() { return channelType; }
    /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
    public String getPackager() { return packager; }
    /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }

        /** Returns whether the embedded Netty POS ISO8583 server should start. */
        public boolean isNettyServerEnabled() { return nettyServerEnabled; }

        /** Sets whether the embedded Netty POS ISO8583 server should start. */
        public void setNettyServerEnabled(boolean nettyServerEnabled) { this.nettyServerEnabled = nettyServerEnabled; }
    }
    
    public static class Bank {
        private String host = "192.168.1.100";
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
            
            // Getters and setters
            public int getMaxAttempts() { return maxAttempts; }
    /** Sets the max attempts. */
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getDelayMs() { return delayMs; }
    /** Sets the delay ms. */
            public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    /** Sets the backoff multiplier. */
            public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        }
        
        // Getters and setters
        public String getHost() { return host; }
    /** Sets the host. */
        public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    /** Sets the port. */
        public void setPort(int port) { this.port = port; }
    public String getChannelType() { return channelType; }
    /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
    public String getPackager() { return packager; }
    /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }
    public int getTimeout() { return timeout; }
    /** Sets the timeout. */
        public void setTimeout(int timeout) { this.timeout = timeout; }
    public int getMaxConnections() { return maxConnections; }
    /** Sets the max connections. */
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    public Retry getRetry() { return retry; }
    /** Sets the retry. */
        public void setRetry(Retry retry) { this.retry = retry; }
    }
    
    // Main getters and setters
    public Pos getPos() { return pos; }
    /** Sets the pos. */
    public void setPos(Pos pos) { this.pos = pos; }
    public Bank getBank() { return bank; }
    /** Sets the bank. */
    public void setBank(Bank bank) { this.bank = bank; }
}
