package com.kevshake.gateway.components;

import org.jpos.q2.Q2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;

/**
 * Service to integrate jPOS Q2 with Spring Boot
 * Manages Q2 lifecycle and deployment
 */
@Service
public class Q2IntegrationService {
    private static final Logger log = LoggerFactory.getLogger(Q2IntegrationService.class);
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    private Q2 q2;
    private Thread q2Thread;
    
    @PostConstruct
    public void startQ2() {
        try {
            // Create deploy directory if it doesn't exist
            File deployDir = new File("src/main/resources/deploy");
            if (!deployDir.exists()) {
                deployDir.mkdirs();
            }
            
            log.info("jPOS Q2 Integration Service started successfully");
            maskedLogger.logSystemEvent("Q2_START", "Q2 integration service started");
            
        } catch (Exception e) {
            log.error("Failed to start Q2 Integration Service", e);
            maskedLogger.logError("Q2_START", "Failed to start Q2 integration", e);
        }
    }
    
    @PreDestroy
    public void stopQ2() {
        try {
            if (q2 != null) {
                q2.shutdown();
            }
            
            if (q2Thread != null && q2Thread.isAlive()) {
                q2Thread.interrupt();
                q2Thread.join(5000); // Wait up to 5 seconds
            }
            
            log.info("jPOS Q2 Integration Service stopped");
            maskedLogger.logSystemEvent("Q2_STOP", "Q2 integration service stopped");
            
        } catch (Exception e) {
            log.error("Error stopping Q2 Integration Service", e);
            maskedLogger.logError("Q2_STOP", "Error stopping Q2 integration", e);
        }
    }
    
    /**
     * Check if Q2 is running
     */
    public boolean isQ2Running() {
        return q2 != null && q2.running();
    }
    
    /**
     * Get Q2 instance (for advanced operations)
     */
    public Q2 getQ2Instance() {
        return q2;
    }
}
