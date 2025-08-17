package com.kevshake.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kevshake.gateway.components.MaskedLogger;

/**
 * PIN Transposition Service
 * Handles conversion of PIN from POS Terminal PIN Key to Gateway Zonal PIN Key
 * This is a critical security function in payment processing
 */
@Service
public class PinTranspositionService {
    private static final Logger log = LoggerFactory.getLogger(PinTranspositionService.class);
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    // Configuration for PIN keys - these should be loaded from secure configuration
    @Value("${iso8583.security.gateway-zonal-key:40763BB5B0B910B5CE3297E58967CD2A}")
    private String gatewayZonalKey;
    
    @Value("${iso8583.security.default-terminal-key:9E4F7FF1F831F1132CD9B6C740B0134C}")
    private String defaultTerminalKey;
    
    /**
     * Transpose PIN from POS Terminal key to Gateway Zonal key
     * 
     * @param encryptedPinBlock PIN block encrypted with terminal key
     * @param pan Primary Account Number
     * @param terminalId Terminal ID (used to determine terminal-specific key if needed)
     * @return PIN block encrypted with gateway zonal key
     */
    public String transposePinToGatewayKey(String encryptedPinBlock, String pan, String terminalId) {
        try {
            // Log the transposition attempt (without sensitive data)
            maskedLogger.logSystemEvent("PIN_TRANSPOSE_START", 
                String.format("Starting PIN transposition for terminal: %s", terminalId));
            
            // Step 1: Get the terminal-specific PIN key (could be from database/config)
            String terminalPinKey = getTerminalPinKey(terminalId);
            
            // Step 2: Decrypt the PIN block using terminal key
            String decryptedPinBlock = TDES.TDES_Decrypt(encryptedPinBlock, terminalPinKey, false);
            
            // Step 3: Decrypt the PIN block to get clear PIN
            String clearPin = TDES.format0decode(decryptedPinBlock, pan);
            
            // Step 4: Create new PIN block with clear PIN and PAN
            String newPinBlock = TDES.format0Encode(clearPin, pan);
            
            // Step 5: Encrypt new PIN block with gateway zonal key
            String gatewayEncryptedPinBlock = TDES.TDES_Encrypt(newPinBlock, gatewayZonalKey, false);
            
            // Log successful transposition
            maskedLogger.logSystemEvent("PIN_TRANSPOSE_SUCCESS", 
                String.format("PIN successfully transposed for terminal: %s", terminalId));
            
            return gatewayEncryptedPinBlock;
            
        } catch (Exception e) {
            // Log error without exposing sensitive data
            maskedLogger.logError("PIN_TRANSPOSE_ERROR",  String.format("PIN transposition failed for terminal: %s", terminalId), e);
            log.error("PIN transposition error for terminal: {}", terminalId, e);
            throw new RuntimeException("PIN transposition failed", e);
        }
    }
    
    /**
     * Transpose PIN from Gateway Zonal key to Bank key (for outgoing transactions)
     * 
     * @param gatewayEncryptedPinBlock PIN block encrypted with gateway zonal key
     * @param pan Primary Account Number
     * @param bankId Bank identifier (used to determine bank-specific key)
     * @return PIN block encrypted with bank key
     */
    public String transposePinToBankKey(String gatewayEncryptedPinBlock, String pan, String bankId) {
        try {
            maskedLogger.logSystemEvent("PIN_TRANSPOSE_BANK_START", 
                String.format("Starting PIN transposition to bank key for bank: %s", bankId));
            
            // Step 1: Get the bank-specific PIN key
            String bankPinKey = getBankPinKey(bankId);
            
            // Step 2: Decrypt PIN block using gateway zonal key
            String decryptedPinBlock = TDES.TDES_Decrypt(gatewayEncryptedPinBlock, gatewayZonalKey, false);
            
            // Step 3: Get clear PIN
            String clearPin = TDES.format0decode(decryptedPinBlock, pan);
            
            // Step 4: Create new PIN block
            String newPinBlock = TDES.format0Encode(clearPin, pan);
            
            // Step 5: Encrypt with bank key
            String bankEncryptedPinBlock = TDES.TDES_Encrypt(newPinBlock, bankPinKey, false);
            
            maskedLogger.logSystemEvent("PIN_TRANSPOSE_BANK_SUCCESS", 
                String.format("PIN successfully transposed to bank key for bank: %s", bankId));
            
            return bankEncryptedPinBlock;
            
        } catch (Exception e) {
            maskedLogger.logError("PIN_TRANSPOSE_BANK_ERROR", String.format("PIN transposition to bank failed for bank: %s", bankId), e);
            log.error("PIN transposition to bank error for bank: {}", bankId, e);
            throw new RuntimeException("PIN transposition to bank failed", e);
        }
    }
    
    /**
     * Validate PIN block format and basic security checks
     * 
     * @param pinBlock PIN block to validate
     * @param pan Primary Account Number
     * @return true if valid, false otherwise
     */
    public boolean validatePinBlock(String pinBlock, String pan) {
        try {
            // Basic format validation
            if (pinBlock == null || pinBlock.length() != 16) {
                log.warn("Invalid PIN block length: {}", pinBlock != null ? pinBlock.length() : 0);
                return false;
            }
            
            // Check if PIN block is all zeros (invalid)
            if ("0000000000000000".equals(pinBlock)) {
                log.warn("PIN block is all zeros - invalid");
                return false;
            }
            
            // Validate PAN
            if (pan == null || pan.length() < 12) {
                log.warn("Invalid PAN length: {}", pan != null ? pan.length() : 0);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating PIN block", e);
            return false;
        }
    }
    
    /**
     * Get terminal-specific PIN key
     * In production, this should retrieve from secure key storage/database
     * 
     * @param terminalId Terminal identifier
     * @return Terminal PIN key
     */
    private String getTerminalPinKey(String terminalId) {
        // TODO: In production, implement secure key retrieval from database/HSM
        // For now, return default terminal key
        
        // Example of terminal-specific key logic:
        // if ("TERM0001".equals(terminalId)) {
        //     return "SPECIFIC_KEY_FOR_TERM0001";
        // }
        
        log.debug("Using default terminal key for terminal: {}", terminalId);
        return defaultTerminalKey;
    }
    
    /**
     * Get bank-specific PIN key
     * In production, this should retrieve from secure key storage/database
     * 
     * @param bankId Bank identifier
     * @return Bank PIN key
     */
    private String getBankPinKey(String bankId) {
        // TODO: In production, implement secure key retrieval from database/HSM
        // Example bank-specific keys:
        
        switch (bankId) {
            case "BANK001":
                return "1234567890ABCDEF1234567890ABCDEF";
            case "BANK002":
                return "FEDCBA0987654321FEDCBA0987654321";
            default:
                // Return a default bank key
                log.debug("Using default bank key for bank: {}", bankId);
                return "ABCDEF1234567890ABCDEF1234567890";
        }
    }
    
    /**
     * Generate Key Check Value for key verification
     * 
     * @param key TDES key
     * @return KCV (Key Check Value)
     */
    public String generateKCV(String key) {
        try {
            return TDES.getKCV(key);
        } catch (Exception e) {
            log.error("Error generating KCV", e);
            maskedLogger.logError("KCV_GENERATION_ERROR", "Failed to generate KCV", e);
            return null;
        }
    }
    
    /**
     * Verify PIN against expected value (for testing/validation)
     * 
     * @param encryptedPinBlock Encrypted PIN block
     * @param pan Primary Account Number
     * @param expectedPin Expected clear PIN (for testing only)
     * @param key Encryption key used
     * @return true if PIN matches expected value
     */
    public boolean verifyPin(String encryptedPinBlock, String pan, String expectedPin, String key) {
        try {
            // Decrypt PIN block
            String decryptedPinBlock = TDES.TDES_Decrypt(encryptedPinBlock, key, false);
            
            // Get clear PIN
            String clearPin = TDES.format0decode(decryptedPinBlock, pan);
            
            // Compare with expected PIN
            boolean matches = expectedPin.equals(clearPin);
            
            if (matches) {
                maskedLogger.logSystemEvent("PIN_VERIFY_SUCCESS", "PIN verification successful");
            } else {
                maskedLogger.logSystemEvent("PIN_VERIFY_FAILED", "PIN verification failed");
            }
            
            return matches;
            
        } catch (Exception e) {
            log.error("Error verifying PIN", e);
            maskedLogger.logError("PIN_VERIFY_ERROR", "PIN verification error", e);
            return false;
        }
    }
    
    /**
     * Get current gateway zonal key (for administrative purposes)
     * 
     * @return Gateway zonal key KCV (not the actual key)
     */
    public String getGatewayKeyKCV() {
        return generateKCV(gatewayZonalKey);
    }
    
    /**
     * Test PIN transposition functionality
     * This method should only be used for testing purposes
     */
    public void testPinTransposition() {
        try {
            String testPan = "4761739001010010";
            String testPin = "1234";
            String testTerminalId = "TERM0001";
            
            log.info("=== PIN Transposition Test ===");
            
            // Create initial PIN block with terminal key
            String terminalKey = getTerminalPinKey(testTerminalId);
            String pinBlock = TDES.format0Encode(testPin, testPan);
            String encryptedPinBlock = TDES.TDES_Encrypt(pinBlock, terminalKey, false);
            
            log.info("Original encrypted PIN block: {}", encryptedPinBlock);
            
            // Transpose to gateway key
            String gatewayPinBlock = transposePinToGatewayKey(encryptedPinBlock, testPan, testTerminalId);
            log.info("Gateway encrypted PIN block: {}", gatewayPinBlock);
            
            // Verify the transposition worked
            boolean verified = verifyPin(gatewayPinBlock, testPan, testPin, gatewayZonalKey);
            log.info("PIN transposition verification: {}", verified ? "SUCCESS" : "FAILED");
            
            // Test bank transposition
            String bankPinBlock = transposePinToBankKey(gatewayPinBlock, testPan, "BANK001");
            log.info("Bank encrypted PIN block: {}", bankPinBlock);
            
            log.info("=== PIN Transposition Test Complete ===");
            
        } catch (Exception e) {
            log.error("PIN transposition test failed", e);
        }
    }
}
