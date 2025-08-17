package com.kevshake.gateway.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.channel.ASCIIChannel;
import com.kevshake.gateway.packagers.BankPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kevshake.gateway.security.PinTranspositionService;
import com.kevshake.gateway.service.ResponseCodeService;
import com.kevshake.gateway.service.BankResponseCodeService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling outgoing communication to bank processors
 * Sends transaction messages to bank using ASCIIChannel and separate packager
 */
@Service
public class BankCommunicationProcessor {
    private static final Logger logger = LogManager.getLogger(BankCommunicationProcessor.class);
    
    @Autowired
    private BankCommunicationConfig config;
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    @Autowired
    private PinTranspositionService pinTranspositionService;
    
    @Autowired
    private ResponseCodeService responseCodeService;
    
    @Autowired
    private BankResponseCodeService bankResponseCodeService;
    
    @Value("${iso8583.security.pin.enable-transposition:true}")
    private boolean enablePinTransposition;
    
    private ASCIIChannel bankChannel;
    private ISOPackager bankPackager;
    private ExecutorService executorService;
    private boolean isConnected = false;
    
    @PostConstruct
    public void initialize() {
        try {
            // Initialize bank packager
            bankPackager = new BankPackager();
            
            // Initialize executor service for async processing
            executorService = Executors.newFixedThreadPool(config.getBank().getMaxConnections());
            
            // Initialize bank channel
            initializeBankChannel();
            
            logger.info("Bank Communication Processor initialized successfully");
            maskedLogger.logSystemEvent("BANK_PROCESSOR_INIT", "Bank communication processor started");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Bank Communication Processor", e);
            maskedLogger.logError("BANK_PROCESSOR_INIT", "Initialization failed", e);
        }
    }
    
    /**
     * Process outgoing transaction to bank
     */
    public CompletableFuture<ISOMsg> processTransactionToBank(ISOMsg originalMsg) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create bank message from original POS message
                ISOMsg bankMsg = createBankMessage(originalMsg);
                
                // Process PIN transposition for bank if message contains PIN data
                processBankPinTransposition(bankMsg);
                
                // Log outgoing transaction
                maskedLogger.logOutgoingTransaction(bankMsg, "BANK");
                
                // Send to bank and get response
                ISOMsg response = sendToBankWithRetry(bankMsg);
                
                // Log bank response
                if (response != null) {
                    maskedLogger.logIncomingTransaction(response, "BANK_RESPONSE");
                    
                    String responseCode = response.getString(39);
                    String stan = response.getString(11);
                    String terminalId = response.getString(41);
                    
                    // Log bank response with detailed narration using bank-specific service
                    bankResponseCodeService.logBankResponseCode(responseCode, 
                        "Bank response received", stan);
                    
                    // Log formatted bank response message
                    String bankResponseMessage = bankResponseCodeService.formatBankResponseMessage(responseCode, terminalId, stan);
                    BankResponseCodeService.BankResponseCodeInfo bankCodeInfo = bankResponseCodeService.getBankResponseCodeInfo(responseCode);
                    
                    // Log based on bank response severity
                    switch (bankCodeInfo.getSeverity()) {
                        case ERROR:
                            logger.error("BANK ERROR RESPONSE: {}", bankResponseMessage);
                            maskedLogger.logError("BANK_ERROR_RESPONSE", bankResponseMessage, null);
                            
                            // Log recommended action for error responses
                            logger.error("RECOMMENDED ACTION: {}", bankCodeInfo.getRecommendedAction());
                            break;
                        case WARN:
                            logger.warn("BANK WARNING RESPONSE: {}", bankResponseMessage);
                            maskedLogger.logSystemEvent("BANK_WARNING_RESPONSE", bankResponseMessage);
                            
                            // Log recommended action for warning responses
                            logger.warn("RECOMMENDED ACTION: {}", bankCodeInfo.getRecommendedAction());
                            break;
                        default:
                            logger.info("BANK SUCCESS RESPONSE: {}", bankResponseMessage);
                            maskedLogger.logSystemEvent("BANK_SUCCESS_RESPONSE", bankResponseMessage);
                            break;
                    }
                    
                    // Generate detailed analysis for non-success responses
                    if (bankResponseCodeService.isBankErrorResponse(responseCode)) {
                        String transactionAmount = originalMsg.getString(4);
                        String analysis = bankResponseCodeService.generateBankResponseAnalysis(responseCode, transactionAmount, "Unknown");
                        logger.debug("BANK RESPONSE ANALYSIS:\n{}", analysis);
                    }
                    
                    maskedLogger.logTransactionResult(stan, responseCode, "Bank response received");
                }
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error processing transaction to bank", e);
                maskedLogger.logError("BANK_TRANSACTION", "Transaction processing failed", e);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Create bank message from POS transaction
     */
    private ISOMsg createBankMessage(ISOMsg posMsg) throws ISOException {
        ISOMsg bankMsg = new ISOMsg();
        bankMsg.setPackager(bankPackager);
        
        // Copy relevant fields from POS message to bank message
        // MTI - Convert POS MTI to appropriate bank MTI
        String posMti = posMsg.getString(0);
        String bankMti = convertMtiForBank(posMti);
        bankMsg.setMTI(bankMti);
        
        // Copy essential fields
        copyFieldIfPresent(posMsg, bankMsg, 2);   // PAN
        copyFieldIfPresent(posMsg, bankMsg, 3);   // Processing Code
        copyFieldIfPresent(posMsg, bankMsg, 4);   // Amount
        copyFieldIfPresent(posMsg, bankMsg, 7);   // Transmission Date Time
        copyFieldIfPresent(posMsg, bankMsg, 11);  // STAN
        copyFieldIfPresent(posMsg, bankMsg, 12);  // Local Time
        copyFieldIfPresent(posMsg, bankMsg, 13);  // Local Date
        copyFieldIfPresent(posMsg, bankMsg, 14);  // Expiration Date
        copyFieldIfPresent(posMsg, bankMsg, 22);  // POS Entry Mode
        copyFieldIfPresent(posMsg, bankMsg, 25);  // POS Capture Code
        copyFieldIfPresent(posMsg, bankMsg, 35);  // Track 2 Data
        copyFieldIfPresent(posMsg, bankMsg, 41);  // Terminal ID
        copyFieldIfPresent(posMsg, bankMsg, 42);  // Merchant ID
        copyFieldIfPresent(posMsg, bankMsg, 43);  // Merchant Name/Location
        copyFieldIfPresent(posMsg, bankMsg, 49);  // Currency Code
        
        // Add bank-specific fields
        bankMsg.set(37, generateRRN());  // Retrieval Reference Number
        bankMsg.set(7, getCurrentTransmissionDateTime());  // Update transmission time
        
        return bankMsg;
    }
    
    /**
     * Convert POS MTI to appropriate bank MTI
     */
    private String convertMtiForBank(String posMti) {
        // Convert POS MTI to bank MTI based on business rules
        switch (posMti) {
            case "0100": // Authorization Request from POS
                return "0100"; // Forward as Authorization Request to bank
            case "0200": // Financial Request from POS
                return "0200"; // Forward as Financial Request to bank
            case "0400": // Reversal Request from POS
                return "0400"; // Forward as Reversal Request to bank
            case "0800": // Network Management from POS
                return "0800"; // Forward as Network Management to bank
            default:
                return posMti; // Default pass-through
        }
    }
    
    /**
     * Send message to bank with retry logic
     */
    private ISOMsg sendToBankWithRetry(ISOMsg msg) {
        BankCommunicationConfig.Bank.Retry retryConfig = config.getBank().getRetry();
        int attempts = 0;
        long delay = retryConfig.getDelayMs();
        
        while (attempts < retryConfig.getMaxAttempts()) {
            try {
                attempts++;
                
                // Ensure connection is established
                if (!isConnected) {
                    connectToBank();
                }
                
                // Send message and wait for response
                bankChannel.send(msg);
                ISOMsg response = bankChannel.receive();
                
                if (response != null) {
                    maskedLogger.logBankCommunication("SEND_SUCCESS", 
                        String.format("Message sent successfully on attempt %d", attempts));
                    return response;
                }
                
            } catch (Exception e) {
                logger.warn("Bank communication attempt {} failed: {}", attempts, e.getMessage());
                maskedLogger.logBankCommunication("SEND_RETRY", 
                    String.format("Attempt %d failed: %s", attempts, e.getMessage()));
                
                // Disconnect and retry
                disconnect();
                
                if (attempts < retryConfig.getMaxAttempts()) {
                    try {
                        Thread.sleep(delay);
                        delay = (long) (delay * retryConfig.getBackoffMultiplier());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        maskedLogger.logBankCommunication("SEND_FAILED", 
            String.format("All %d attempts failed", attempts));
        return null;
    }
    
    /**
     * Initialize bank channel connection
     */
    private void initializeBankChannel() {
        try {
            BankCommunicationConfig.Bank bankConfig = config.getBank();
            bankChannel = new ASCIIChannel(bankConfig.getHost(), bankConfig.getPort(), bankPackager);
            bankChannel.setTimeout(bankConfig.getTimeout());
        } catch (SocketException e) {
            logger.error("Error initializing bank channel", e);
            maskedLogger.logError("BANK_CHANNEL_INIT", "Failed to initialize bank channel", e);
        }
    }
    
    /**
     * Connect to bank
     */
    private void connectToBank() throws IOException, SocketException {
        if (!isConnected) {
            bankChannel.connect();
            isConnected = true;
            maskedLogger.logBankCommunication("CONNECT", "Connected to bank successfully");
        }
    }
    
    /**
     * Disconnect from bank
     */
    private void disconnect() {
        if (isConnected) {
            try {
                bankChannel.disconnect();
                maskedLogger.logBankCommunication("DISCONNECT", "Disconnected from bank");
            } catch (Exception e) {
                logger.warn("Error disconnecting from bank", e);
            } finally {
                isConnected = false;
            }
        }
    }
    
    /**
     * Copy field from source to destination if present
     */
    private void copyFieldIfPresent(ISOMsg source, ISOMsg destination, int fieldNumber) {
        try {
            if (source.hasField(fieldNumber)) {
                destination.set(fieldNumber, source.getString(fieldNumber));
            }
        } catch (Exception e) {
            logger.warn("Error copying field {}: {}", fieldNumber, e.getMessage());
        }
    }
    
    /**
     * Generate Retrieval Reference Number
     */
    private String generateRRN() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
    
    /**
     * Get current transmission date time
     */
    private String getCurrentTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }
    
    /**
     * Check if bank is connected
     */
    public boolean isBankConnected() {
        return isConnected && bankChannel != null && bankChannel.isConnected();
    }
    
    /**
     * Get bank connection status
     */
    public String getBankConnectionStatus() {
        if (isBankConnected()) {
            return String.format("Connected to %s:%d", 
                config.getBank().getHost(), config.getBank().getPort());
        } else {
            return "Disconnected";
        }
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Bank Communication Processor");
        
        // Disconnect from bank
        disconnect();
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        maskedLogger.logSystemEvent("BANK_PROCESSOR_SHUTDOWN", "Bank communication processor stopped");
    }
    
    /**
     * Process PIN transposition for outgoing bank messages
     * Converts PIN from Gateway Zonal key to Bank-specific key
     */
    private void processBankPinTransposition(ISOMsg bankMsg) {
        try {
            // Check if PIN transposition is enabled
            if (!enablePinTransposition) {
                logger.debug("PIN transposition is disabled for bank communication");
                return;
            }
            
            // Check if message contains PIN data (field 52)
            if (!bankMsg.hasField(52)) {
                logger.debug("Bank message does not contain PIN data (field 52)");
                return;
            }
            
            String gatewayPinBlock = bankMsg.getString(52);
            String pan = bankMsg.getString(2);
            String merchantId = bankMsg.getString(42); // Use merchant ID to determine bank
            
            // Validate required fields
            if (pan == null || pan.isEmpty()) {
                logger.warn("Cannot transpose PIN for bank: PAN (field 2) is missing");
                maskedLogger.logError("BANK_PIN_TRANSPOSE", "PAN missing for bank PIN transposition", null);
                return;
            }
            
            // Determine bank ID from merchant ID or use default
            String bankId = determineBankId(merchantId);
            
            // Validate PIN block
            if (!pinTranspositionService.validatePinBlock(gatewayPinBlock, pan)) {
                logger.warn("Invalid PIN block format for bank transposition");
                maskedLogger.logError("BANK_PIN_TRANSPOSE", "Invalid PIN block format for bank", null);
                return;
            }
            
            logger.debug("Starting PIN transposition to bank key for bank: {}", bankId);
            
            // Transpose PIN from gateway zonal key to bank key
            String bankPinBlock = pinTranspositionService.transposePinToBankKey(
                gatewayPinBlock, pan, bankId);
            
            // Update message with bank PIN block
            bankMsg.set(52, bankPinBlock);
            
            logger.debug("Bank PIN transposition completed for bank: {}", bankId);
            maskedLogger.logSystemEvent("BANK_PIN_TRANSPOSE_SUCCESS", 
                String.format("PIN transposed to bank key for bank: %s", bankId));
            
        } catch (Exception e) {
            logger.error("Error during bank PIN transposition", e);
            maskedLogger.logError("BANK_PIN_TRANSPOSE_ERROR", "Bank PIN transposition failed", e);
            throw new RuntimeException("Bank PIN transposition failed", e);
        }
    }
    
    /**
     * Determine bank ID from merchant information
     * In production, this would use routing tables or merchant configuration
     */
    private String determineBankId(String merchantId) {
        // Simple routing logic - in production this would be more sophisticated
        if (merchantId == null || merchantId.isEmpty()) {
            return "DEFAULT_BANK";
        }
        
        // Example routing based on merchant ID patterns
        if (merchantId.startsWith("MERCH001") || merchantId.startsWith("BANK001")) {
            return "BANK001";
        } else if (merchantId.startsWith("MERCH002") || merchantId.startsWith("BANK002")) {
            return "BANK002";
        } else {
            return "DEFAULT_BANK";
        }
    }
}
