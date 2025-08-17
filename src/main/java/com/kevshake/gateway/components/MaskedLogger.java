package com.kevshake.gateway.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for masked logging of ISO8583 messages
 * Ensures sensitive data is masked in logs for security compliance
 */
@Component
public class MaskedLogger {
    private static final Logger logger = LogManager.getLogger(MaskedLogger.class);
    
    // Fields that should be masked in logs for security
    private static final Set<Integer> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
        2,   // PAN (Primary Account Number)
        14,  // Expiration Date
        35,  // Track 2 Data
        45,  // Track 1 Data
        55,  // EMV Data
        120, // Additional POS Data
        126  // Private Use Fields
    ));
    
    // Fields that should be partially masked (show first/last few characters)
    private static final Set<Integer> PARTIALLY_MASKED_FIELDS = new HashSet<>(Arrays.asList(
        37,  // Retrieval Reference Number
        41,  // Terminal ID
        42   // Merchant ID
    ));
    
    /**
     * Log incoming transaction with masked sensitive fields
     */
    public void logIncomingTransaction(ISOMsg msg, String source) {
        try {
            logger.info("=== INCOMING TRANSACTION FROM {} ===", source);
            logger.info("MTI: {}", msg.getString(0));
            logMaskedFields(msg);
            logger.info("=== END INCOMING TRANSACTION ===");
        } catch (Exception e) {
            logger.error("Error logging incoming transaction", e);
        }
    }
    
    /**
     * Log outgoing transaction with masked sensitive fields
     */
    public void logOutgoingTransaction(ISOMsg msg, String destination) {
        try {
            logger.info("=== OUTGOING TRANSACTION TO {} ===", destination);
            logger.info("MTI: {}", msg.getString(0));
            logMaskedFields(msg);
            logger.info("=== END OUTGOING TRANSACTION ===");
        } catch (Exception e) {
            logger.error("Error logging outgoing transaction", e);
        }
    }
    
    /**
     * Log transaction processing result
     */
    public void logTransactionResult(String stan, String responseCode, String description) {
        logger.info("Transaction STAN: {} - Response: {} - {}", stan, responseCode, description);
    }
    
    /**
     * Log bank communication event
     */
    public void logBankCommunication(String event, String details) {
        Logger bankLogger = LogManager.getLogger("com.kevshake.gateway.components.BankCommunicationProcessor");
        bankLogger.info("Bank Communication - {}: {}", event, details);
    }
    
    /**
     * Log all message fields with appropriate masking
     */
    private void logMaskedFields(ISOMsg msg) {
        try {
            for (int i = 1; i <= 128; i++) {
                if (msg.hasField(i)) {
                    String value = msg.getString(i);
                    String maskedValue = maskFieldValue(i, value);
                    logger.info("Field {}: {}", String.format("%03d", i), maskedValue);
                }
            }
        } catch (Exception e) {
            logger.error("Error logging masked fields", e);
        }
    }
    
    /**
     * Mask field value based on field type and security requirements
     */
    private String maskFieldValue(int fieldNumber, String value) {
        if (value == null || value.isEmpty()) {
            return "null";
        }
        
        if (SENSITIVE_FIELDS.contains(fieldNumber)) {
            return maskSensitiveField(fieldNumber, value);
        } else if (PARTIALLY_MASKED_FIELDS.contains(fieldNumber)) {
            return maskPartialField(value);
        } else {
            return value; // No masking needed
        }
    }
    
    /**
     * Completely mask sensitive fields
     */
    private String maskSensitiveField(int fieldNumber, String value) {
        switch (fieldNumber) {
            case 2: // PAN - show first 4 and last 4 digits
                return maskPan(value);
            case 14: // Expiration Date - mask completely
                return "****";
            case 35: // Track 2 Data - mask completely except separator
                return maskTrack2Data(value);
            default:
                return "*".repeat(Math.min(value.length(), 20)); // Mask with asterisks
        }
    }
    
    /**
     * Partially mask fields (show first/last few characters)
     */
    private String maskPartialField(String value) {
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        } else if (value.length() <= 8) {
            return value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2);
        } else {
            return value.substring(0, 3) + "*".repeat(value.length() - 6) + value.substring(value.length() - 3);
        }
    }
    
    /**
     * Mask PAN showing first 4 and last 4 digits
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        return pan.substring(0, 4) + "*".repeat(pan.length() - 8) + pan.substring(pan.length() - 4);
    }
    
    /**
     * Mask Track 2 data while preserving structure
     */
    private String maskTrack2Data(String track2) {
        if (track2 == null || track2.isEmpty()) {
            return "****";
        }
        
        // Find separator (usually '=' or 'D')
        int separatorIndex = -1;
        for (int i = 0; i < track2.length(); i++) {
            char c = track2.charAt(i);
            if (c == '=' || c == 'D' || c == 'd') {
                separatorIndex = i;
                break;
            }
        }
        
        if (separatorIndex > 0) {
            String pan = track2.substring(0, separatorIndex);
            String remaining = track2.substring(separatorIndex);
            return maskPan(pan) + remaining.charAt(0) + "*".repeat(remaining.length() - 1);
        } else {
            return "*".repeat(Math.min(track2.length(), 20));
        }
    }
    
    /**
     * Log system events
     */
    public void logSystemEvent(String event, String details) {
        logger.info("System Event - {}: {}", event, details);
    }
    
    /**
     * Log errors with context
     */
    public void logError(String operation, String context, Exception e) {
        logger.error("Error in {} - Context: {} - Error: {}", operation, context, e.getMessage(), e);
    }
}
