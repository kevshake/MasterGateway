package com.kevshake.gateway.cmponents;

import java.util.Map;

import org.jpos.iso.ISOPackager;
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

    @Bean
    public ISOPackager isoPackager() throws Exception {
        // Use a built-in ISO 8583 packager that doesn't require XML configuration
        return new org.jpos.iso.packager.ISO87APackager();
    }
}