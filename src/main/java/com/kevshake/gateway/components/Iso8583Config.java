package com.kevshake.gateway.components;

import java.util.Map;

import org.jpos.iso.ISOPackager;
import com.kevshake.gateway.packagers.POSPackager;
import com.kevshake.gateway.packagers.BankPackager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "iso8583")
public class Iso8583Config {
    private String packager;
    private Map<Integer, String> fields;

    /**
     * POS Terminal Packager (for incoming transactions)
     */
    @Bean(name = "posPackager")
    public ISOPackager posPackager() throws Exception {
        // Use custom POS packager for POS terminals
        return new POSPackager();
    }
    
    /**
     * Bank Communication Packager (for outgoing transactions)
     */
    @Bean(name = "bankPackager")
    public ISOPackager bankPackager() throws Exception {
        // Use custom bank packager for bank communication
        return new BankPackager();
    }
    
    /**
     * Default packager (maintains backward compatibility)
     */
    @Bean
    public ISOPackager isoPackager() throws Exception {
        // Default to POS packager for backward compatibility
        return posPackager();
    }
    
    // Getters and setters
    public String getPackager() {
        return packager;
    }
    
    public void setPackager(String packager) {
        this.packager = packager;
    }
    
    public Map<Integer, String> getFields() {
        return fields;
    }
    
    public void setFields(Map<Integer, String> fields) {
        this.fields = fields;
    }
}