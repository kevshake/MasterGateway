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
        public void setPort(int port) { this.port = port; }
        
        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        
        public String getPackager() { return packager; }
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
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            
            public int getDelayMs() { return delayMs; }
            public void setDelayMs(int delayMs) { this.delayMs = delayMs; }
            
            public double getBackoffMultiplier() { return backoffMultiplier; }
            public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        }
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        
        public String getPackager() { return packager; }
        public void setPackager(String packager) { this.packager = packager; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }
    }
    
    // Main getters and setters
    public Pos getPos() { return pos; }
    public void setPos(Pos pos) { this.pos = pos; }
    
    public Bank getBank() { return bank; }
    public void setBank(Bank bank) { this.bank = bank; }
}
