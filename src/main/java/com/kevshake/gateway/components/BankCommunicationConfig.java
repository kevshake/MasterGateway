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
        
        // Getters and setters
        public int getPort() { return port; }
    /** Sets the port. */
        public void setPort(int port) { this.port = port; }
        
    /** Returns the channel type. */
        public String getChannelType() { return channelType; }
    /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
        
    /** Returns the packager. */
        public String getPackager() { return packager; }
    /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }
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
            
    /** Returns the delay ms. */
            public int getDelayMs() { return delayMs; }
    /** Sets the delay ms. */
            public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
            
    /** Returns the backoff multiplier. */
            public double getBackoffMultiplier() { return backoffMultiplier; }
    /** Sets the backoff multiplier. */
            public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        }
        
        // Getters and setters
        public String getHost() { return host; }
    /** Sets the host. */
        public void setHost(String host) { this.host = host; }
        
    /** Returns the port. */
        public int getPort() { return port; }
    /** Sets the port. */
        public void setPort(int port) { this.port = port; }
        
    /** Returns the channel type. */
        public String getChannelType() { return channelType; }
    /** Sets the channel type. */
        public void setChannelType(String channelType) { this.channelType = channelType; }
        
    /** Returns the packager. */
        public String getPackager() { return packager; }
    /** Sets the packager. */
        public void setPackager(String packager) { this.packager = packager; }
        
    /** Returns the timeout. */
        public int getTimeout() { return timeout; }
    /** Sets the timeout. */
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
    /** Returns the max connections. */
        public int getMaxConnections() { return maxConnections; }
    /** Sets the max connections. */
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
    /** Returns the retry. */
        public Retry getRetry() { return retry; }
    /** Sets the retry. */
        public void setRetry(Retry retry) { this.retry = retry; }
    }
    
    // Main getters and setters
    public Pos getPos() { return pos; }
    /** Sets the pos. */
    public void setPos(Pos pos) { this.pos = pos; }
    
    /** Returns the bank. */
    public Bank getBank() { return bank; }
    /** Sets the bank. */
    public void setBank(Bank bank) { this.bank = bank; }
}
