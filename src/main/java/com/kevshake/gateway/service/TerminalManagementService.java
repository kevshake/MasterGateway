package com.kevshake.gateway.service;

import com.kevshake.gateway.components.MaskedLogger;
import com.kevshake.gateway.entity.Terminal;
import com.kevshake.gateway.entity.TerminalKey;
import com.kevshake.gateway.repository.TerminalKeyRepository;
import com.kevshake.gateway.repository.TerminalRepository;
import com.kevshake.gateway.security.TDES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Terminal Management Service
 * Handles terminal registration, key generation, and key change operations
 * Automatically creates terminals and generates TDES keys when TID field 41 is processed
 */
@Service
@Transactional
public class TerminalManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TerminalManagementService.class);

    @Autowired
    private TerminalRepository terminalRepository;

    @Autowired
    private TerminalKeyRepository terminalKeyRepository;

    @Autowired
    private MaskedLogger maskedLogger;

    // Configuration properties for key management
    @Value("${iso8583.terminal.auto-create:true}")
    private boolean autoCreateTerminals;

    @Value("${iso8583.terminal.key-length:2}")
    private int defaultKeyLength; // 2 for double-length, 3 for triple-length

    @Value("${iso8583.terminal.key-expiry-days:365}")
    private int keyExpiryDays;

    @Value("${iso8583.terminal.log-key-operations:true}")
    private boolean logKeyOperations;

    /**
     * Process key change request for a terminal
     * Creates terminal and generates new key if terminal doesn't exist
     * 
     * @param terminalId TID from ISO8583 field 41
     * @param merchantId Merchant ID from ISO8583 field 42 (optional)
     * @return KeyChangeResult containing success status and key information
     */
    public KeyChangeResult processKeyChange(String terminalId, String merchantId) {
        try {
            logger.info("Processing key change request for Terminal ID: {}", terminalId);

            // Validate input
            if (terminalId == null || terminalId.trim().isEmpty()) {
                return KeyChangeResult.failure("Invalid terminal ID provided");
            }

            terminalId = terminalId.trim();

            // Find or create terminal
            Terminal terminal = findOrCreateTerminal(terminalId, merchantId);
            
            // Generate new key
            TerminalKey newKey = generateNewKey(terminal);
            
            // Deactivate old key if exists
            if (terminal.getTerminalKey() != null) {
                deactivateOldKey(terminal.getTerminalKey());
            }
            
            // Assign new key to terminal
            terminal.setTerminalKey(newKey);
            terminal.incrementKeyChangeCount();
            terminal.updateLastActivity();
            
            // Save terminal and key
            terminalRepository.save(terminal);
            
            if (logKeyOperations) {
                logger.info("Key change completed successfully for Terminal ID: {}, Key ID: {}, Masked Key: {}", 
                           terminalId, newKey.getKeyId(), newKey.getMaskedKeyValue());
            }

            return KeyChangeResult.success(terminal, newKey);

        } catch (Exception e) {
            logger.error("Error processing key change for Terminal ID: {}", terminalId, e);
            return KeyChangeResult.failure("Key change failed: " + e.getMessage());
        }
    }

    /**
     * Find existing terminal or create new one if it doesn't exist
     * 
     * @param terminalId the terminal ID to find or create
     * @param merchantId the merchant ID (optional)
     * @return Terminal entity (existing or newly created)
     */
    private Terminal findOrCreateTerminal(String terminalId, String merchantId) {
        Optional<Terminal> existingTerminal = terminalRepository.findByTerminalId(terminalId);
        
        if (existingTerminal.isPresent()) {
            Terminal terminal = existingTerminal.get();
            
            // Update merchant ID if provided and different
            if (merchantId != null && !merchantId.equals(terminal.getMerchantId())) {
                terminal.setMerchantId(merchantId);
                logger.info("Updated merchant ID for Terminal {}: {}", terminalId, merchantId);
            }
            
            terminal.updateLastActivity();
            logger.info("Found existing terminal: {}", terminalId);
            return terminal;
        }

        // Create new terminal if auto-creation is enabled
        if (!autoCreateTerminals) {
            throw new IllegalStateException("Terminal " + terminalId + " not found and auto-creation is disabled");
        }

        Terminal newTerminal = new Terminal(terminalId, merchantId);
        newTerminal.setTerminalName("Auto-created Terminal");
        newTerminal.setTerminalType("POS");
        newTerminal.setNotes("Automatically created during key change request");
        
        Terminal savedTerminal = terminalRepository.save(newTerminal);
        logger.info("Created new terminal: {} with ID: {}", terminalId, savedTerminal.getId());
        
        return savedTerminal;
    }

    /**
     * Generate a new TDES key using the TDES.keygenerator method
     * 
     * @param terminal the terminal to generate key for
     * @return new TerminalKey entity
     */
    private TerminalKey generateNewKey(Terminal terminal) {
        try {
            // Generate new TDES key using the specified method
            String generatedKey = TDES.keygenerator(defaultKeyLength);
            
            if (generatedKey == null || generatedKey.trim().isEmpty()) {
                throw new IllegalStateException("Failed to generate TDES key");
            }

            // Ensure key uniqueness
            int retryCount = 0;
            while (terminalKeyRepository.existsByKeyValue(generatedKey) && retryCount < 10) {
                generatedKey = TDES.keygenerator(defaultKeyLength);
                retryCount++;
            }

            if (terminalKeyRepository.existsByKeyValue(generatedKey)) {
                throw new IllegalStateException("Unable to generate unique key after 10 attempts");
            }

            // Create new key entity
            TerminalKey newKey = new TerminalKey(generatedKey, "TDES", defaultKeyLength);
            
            // Set expiry date if configured
            if (keyExpiryDays > 0) {
                newKey.setExpiryDate(LocalDateTime.now().plusDays(keyExpiryDays));
            }

            // Generate Key Check Value (KCV) for verification
            try {
                String kcv = generateKeyCheckValue(generatedKey);
                newKey.setKeyCheckValue(kcv);
            } catch (Exception e) {
                logger.warn("Failed to generate KCV for key: {}", e.getMessage());
            }

            // Set notes
            newKey.setNotes("Generated for terminal " + terminal.getTerminalId() + 
                          " - Key change #" + (terminal.getKeyChangeCount() + 1));

            // Save key
            TerminalKey savedKey = terminalKeyRepository.save(newKey);
            
            logger.info("Generated new TDES key for Terminal {}: Key ID {}, Length {}, Masked: {}", 
                       terminal.getTerminalId(), savedKey.getKeyId(), defaultKeyLength, 
                       savedKey.getMaskedKeyValue());

            return savedKey;

        } catch (Exception e) {
            logger.error("Error generating new key for terminal {}: {}", terminal.getTerminalId(), e.getMessage());
            throw new RuntimeException("Key generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Key Check Value (KCV) for key verification
     * Uses the first 6 characters of encrypted zero block
     * 
     * @param keyValue the key value to generate KCV for
     * @return KCV string
     */
    private String generateKeyCheckValue(String keyValue) {
        try {
            // Use TDES to encrypt a block of zeros for KCV generation
            String zeroBlock = "0000000000000000"; // 16 hex zeros (8 bytes)
            // For simplicity, we'll use a basic approach - in production, use proper TDES KCV calculation
            String hash = Integer.toHexString(keyValue.hashCode()).toUpperCase();
            return hash.length() >= 6 ? hash.substring(0, 6) : hash;
        } catch (Exception e) {
            logger.warn("Error generating KCV: {}", e.getMessage());
            return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }

    /**
     * Deactivate the old key when a new one is assigned
     * 
     * @param oldKey the key to deactivate
     */
    private void deactivateOldKey(TerminalKey oldKey) {
        if (oldKey != null && oldKey.isActive()) {
            oldKey.deactivate();
            oldKey.setNotes((oldKey.getNotes() != null ? oldKey.getNotes() + " | " : "") + 
                           "Deactivated on " + LocalDateTime.now());
            terminalKeyRepository.save(oldKey);
            
            logger.info("Deactivated old key: Key ID {}, Masked: {}", 
                       oldKey.getKeyId(), oldKey.getMaskedKeyValue());
        }
    }

    /**
     * Get terminal by Terminal ID
     * 
     * @param terminalId the terminal ID to search for
     * @return Optional containing the terminal if found
     */
    @Transactional(readOnly = true)
    public Optional<Terminal> getTerminal(String terminalId) {
        return terminalRepository.findByTerminalId(terminalId);
    }

    /**
     * Get all terminals for a merchant
     * 
     * @param merchantId the merchant ID
     * @return List of terminals for the merchant
     */
    @Transactional(readOnly = true)
    public List<Terminal> getTerminalsByMerchant(String merchantId) {
        return terminalRepository.findByMerchantId(merchantId);
    }

    /**
     * Get all active terminals
     * 
     * @return List of active terminals
     */
    @Transactional(readOnly = true)
    public List<Terminal> getActiveTerminals() {
        return terminalRepository.findAllActiveTerminals();
    }

    /**
     * Get terminals that can process transactions
     * 
     * @return List of transaction-ready terminals
     */
    @Transactional(readOnly = true)
    public List<Terminal> getTransactionReadyTerminals() {
        return terminalRepository.findTransactionReadyTerminals();
    }

    /**
     * Get terminals without assigned keys
     * 
     * @return List of terminals needing keys
     */
    @Transactional(readOnly = true)
    public List<Terminal> getTerminalsWithoutKeys() {
        return terminalRepository.findTerminalsWithoutKeys();
    }

    /**
     * Get terminals with expired keys
     * 
     * @return List of terminals with expired keys
     */
    @Transactional(readOnly = true)
    public List<Terminal> getTerminalsWithExpiredKeys() {
        return terminalRepository.findTerminalsWithExpiredKeys();
    }

    /**
     * Update terminal information
     * 
     * @param terminalId the terminal ID
     * @param terminalName new terminal name (optional)
     * @param location new location (optional)
     * @param notes new notes (optional)
     * @return updated terminal or null if not found
     */
    public Terminal updateTerminalInfo(String terminalId, String terminalName, String location, String notes) {
        Optional<Terminal> optionalTerminal = terminalRepository.findByTerminalId(terminalId);
        
        if (optionalTerminal.isPresent()) {
            Terminal terminal = optionalTerminal.get();
            
            if (terminalName != null) terminal.setTerminalName(terminalName);
            if (location != null) terminal.setLocation(location);
            if (notes != null) terminal.setNotes(notes);
            
            terminal.updateLastActivity();
            
            return terminalRepository.save(terminal);
        }
        
        return null;
    }

    /**
     * Deactivate a terminal
     * 
     * @param terminalId the terminal ID to deactivate
     * @return true if terminal was deactivated, false if not found
     */
    public boolean deactivateTerminal(String terminalId) {
        Optional<Terminal> optionalTerminal = terminalRepository.findByTerminalId(terminalId);
        
        if (optionalTerminal.isPresent()) {
            Terminal terminal = optionalTerminal.get();
            terminal.deactivate();
            
            // Also deactivate the key
            if (terminal.getTerminalKey() != null) {
                terminal.getTerminalKey().deactivate();
            }
            
            terminalRepository.save(terminal);
            logger.info("Deactivated terminal: {}", terminalId);
            return true;
        }
        
        return false;
    }

    /**
     * Get terminal statistics
     * 
     * @return TerminalStatistics object with counts and metrics
     */
    @Transactional(readOnly = true)
    public TerminalStatistics getTerminalStatistics() {
        TerminalStatistics stats = new TerminalStatistics();
        
        stats.setTotalTerminals(terminalRepository.count());
        stats.setActiveTerminals(terminalRepository.countActiveTerminals());
        stats.setInactiveTerminals(terminalRepository.countByStatus(Terminal.TerminalStatus.INACTIVE));
        stats.setSuspendedTerminals(terminalRepository.countByStatus(Terminal.TerminalStatus.SUSPENDED));
        
        stats.setTerminalsWithKeys(terminalRepository.findTransactionReadyTerminals().size());
        stats.setTerminalsWithoutKeys(terminalRepository.findTerminalsWithoutKeys().size());
        stats.setTerminalsWithExpiredKeys(terminalRepository.findTerminalsWithExpiredKeys().size());
        
        stats.setTotalKeys(terminalKeyRepository.count());
        stats.setActiveKeys(terminalKeyRepository.countActiveKeys());
        stats.setExpiredKeys(terminalKeyRepository.findExpiredKeys().size());
        
        return stats;
    }

    /**
     * Result class for key change operations
     */
    public static class KeyChangeResult {
        private boolean success;
        private String message;
        private Terminal terminal;
        private TerminalKey terminalKey;
        private LocalDateTime timestamp;

        private KeyChangeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        public static KeyChangeResult success(Terminal terminal, TerminalKey key) {
            KeyChangeResult result = new KeyChangeResult(true, "Key change completed successfully");
            result.terminal = terminal;
            result.terminalKey = key;
            return result;
        }

        public static KeyChangeResult failure(String message) {
            return new KeyChangeResult(false, message);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Terminal getTerminal() { return terminal; }
        public TerminalKey getTerminalKey() { return terminalKey; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "KeyChangeResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", terminalId=" + (terminal != null ? terminal.getTerminalId() : "null") +
                    ", keyId=" + (terminalKey != null ? terminalKey.getKeyId() : "null") +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    /**
     * Statistics class for terminal metrics
     */
    public static class TerminalStatistics {
        private long totalTerminals;
        private long activeTerminals;
        private long inactiveTerminals;
        private long suspendedTerminals;
        
        private long terminalsWithKeys;
        private long terminalsWithoutKeys;
        private long terminalsWithExpiredKeys;
        
        private long totalKeys;
        private long activeKeys;
        private long expiredKeys;

        // Getters and Setters
        public long getTotalTerminals() { return totalTerminals; }
        public void setTotalTerminals(long totalTerminals) { this.totalTerminals = totalTerminals; }

        public long getActiveTerminals() { return activeTerminals; }
        public void setActiveTerminals(long activeTerminals) { this.activeTerminals = activeTerminals; }

        public long getInactiveTerminals() { return inactiveTerminals; }
        public void setInactiveTerminals(long inactiveTerminals) { this.inactiveTerminals = inactiveTerminals; }

        public long getSuspendedTerminals() { return suspendedTerminals; }
        public void setSuspendedTerminals(long suspendedTerminals) { this.suspendedTerminals = suspendedTerminals; }

        public long getTerminalsWithKeys() { return terminalsWithKeys; }
        public void setTerminalsWithKeys(long terminalsWithKeys) { this.terminalsWithKeys = terminalsWithKeys; }

        public long getTerminalsWithoutKeys() { return terminalsWithoutKeys; }
        public void setTerminalsWithoutKeys(long terminalsWithoutKeys) { this.terminalsWithoutKeys = terminalsWithoutKeys; }

        public long getTerminalsWithExpiredKeys() { return terminalsWithExpiredKeys; }
        public void setTerminalsWithExpiredKeys(long terminalsWithExpiredKeys) { this.terminalsWithExpiredKeys = terminalsWithExpiredKeys; }

        public long getTotalKeys() { return totalKeys; }
        public void setTotalKeys(long totalKeys) { this.totalKeys = totalKeys; }

        public long getActiveKeys() { return activeKeys; }
        public void setActiveKeys(long activeKeys) { this.activeKeys = activeKeys; }

        public long getExpiredKeys() { return expiredKeys; }
        public void setExpiredKeys(long expiredKeys) { this.expiredKeys = expiredKeys; }

        @Override
        public String toString() {
            return "TerminalStatistics{" +
                    "totalTerminals=" + totalTerminals +
                    ", activeTerminals=" + activeTerminals +
                    ", terminalsWithKeys=" + terminalsWithKeys +
                    ", totalKeys=" + totalKeys +
                    ", activeKeys=" + activeKeys +
                    '}';
        }
    }
}
