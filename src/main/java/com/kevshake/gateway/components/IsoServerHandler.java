package com.kevshake.gateway.components;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import com.kevshake.gateway.security.PinTranspositionService;
import com.kevshake.gateway.security.CardValidationService;
import com.kevshake.gateway.service.TerminalManagementService;
import com.kevshake.gateway.service.ResponseCodeService;

//Handler: Process messages
@Component
public class IsoServerHandler extends SimpleChannelInboundHandler<ISOMsg> {
    private static final Logger log = LoggerFactory.getLogger(IsoServerHandler.class);
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    @Autowired
    private BankCommunicationProcessor bankProcessor;
    
    @Autowired
    private TransactionProcessor transactionProcessor;
    
    @Autowired
    private PinTranspositionService pinTranspositionService;
    
    @Autowired
    private CardValidationService cardValidationService;
    
    @Autowired
    private TerminalManagementService terminalManagementService;
    
    @Autowired
    private ResponseCodeService responseCodeService;
    
    @Value("${iso8583.security.pin.enable-transposition:true}")
    private boolean enablePinTransposition;
    
    @Value("${iso8583.security.card.enable-validation:true}")
    private boolean enableCardValidation;
    
    @Value("${iso8583.terminal.enable-key-change:true}")
    private boolean enableKeyChange;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ISOMsg msg) {
        try {
            // Get MTI (Message Type Indicator)
            String mti = msg.getString(0);
            log.info("Received message with MTI: {}", mti);
            
            // Log incoming transaction with masked logging
            maskedLogger.logIncomingTransaction(msg, "POS_TERMINAL");
            
            // Validate card number using Luhn algorithm
            if (!validateCardNumber(msg)) {
                String terminalId = msg.getString(41);
                String stan = msg.getString(11);
                log.warn("Card validation failed for terminal: {} STAN: {}", terminalId, stan);
                responseCodeService.logResponseCode("14", "Card validation failed - Luhn algorithm check", stan);
                sendErrorResponse(ctx, msg, "14"); // Invalid card number
                return;
            }
            
            // Process PIN transposition if message contains PIN data
            processPinTransposition(msg);
            
            // Process transaction and forward to bank if needed
            processAndForwardTransaction(ctx, msg);
            
            switch (mti) {
                case "0200": // Authorization Request
                    handleAuthorizationRequest(ctx, msg);
                    break;
                case "0220": // Authorization Advice
                    handleAuthorizationAdvice(ctx, msg);
                    break;
                case "0400": // Reversal Request
                    handleReversalRequest(ctx, msg);
                    break;
                case "0420": // Reversal Advice
                    handleReversalAdvice(ctx, msg);
                    break;
                case "0800": // Network Management Request
                    handleNetworkManagement(ctx, msg);
                    break;
                case "0100": // Authorization Request (ATM/POS)
                    handlePosAuthorization(ctx, msg);
                    break;
                default:
                    log.warn("Unsupported MTI: {}", mti);
                    handleUnsupportedMessage(ctx, msg);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            handleError(ctx, msg, e);
        }
    }
    
    /**
     * Process transaction and forward to bank if required
     */
    private void processAndForwardTransaction(ChannelHandlerContext ctx, ISOMsg msg) {
        try {
            String mti = msg.getString(0);
            String stan = msg.getString(11);
            
            // Determine if transaction should be forwarded to bank
            if (shouldForwardToBank(mti)) {
                maskedLogger.logSystemEvent("BANK_FORWARD", 
                    String.format("Forwarding transaction STAN: %s to bank", stan));
                
                // Forward to bank asynchronously
                CompletableFuture<ISOMsg> bankResponse = bankProcessor.processTransactionToBank(msg);
                
                // Handle bank response (this will be processed asynchronously)
                bankResponse.thenAccept(response -> {
                    if (response != null) {
                        maskedLogger.logSystemEvent("BANK_RESPONSE", 
                            String.format("Received bank response for STAN: %s", stan));
                        // Bank response can be used for additional processing or logging
                    } else {
                        maskedLogger.logSystemEvent("BANK_TIMEOUT", 
                            String.format("Bank timeout for STAN: %s", stan));
                    }
                });
            }
        } catch (Exception e) {
            maskedLogger.logError("BANK_FORWARD", "Error forwarding transaction to bank", e);
        }
    }
    
    /**
     * Determine if transaction should be forwarded to bank
     */
    private boolean shouldForwardToBank(String mti) {
        // Forward financial transactions to bank
        switch (mti) {
            case "0100": // Authorization Request
            case "0200": // Financial Request
            case "0400": // Reversal Request
                return true;
            case "0800": // Network Management - may or may not forward based on processing code
            default:
                return false;
        }
    }

    private void handleAuthorizationRequest(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing Authorization Request");
        
        try {
            // Extract key fields
            String pan = msg.getString(2);           // Primary Account Number
            String processingCode = msg.getString(3); // Processing Code
            String amount = msg.getString(4);        // Transaction Amount
            String stan = msg.getString(11);         // System Trace Audit Number
            String localTime = msg.getString(12);    // Local Transaction Time
            String localDate = msg.getString(13);    // Local Transaction Date
            
            log.info("PAN: {}, Processing Code: {}, Amount: {}, STAN: {}", 
                    maskPan(pan), processingCode, amount, stan);
            
            // Business logic based on processing code
            String responseCode = processTransaction(processingCode, amount, pan);
            
            // Create response message (0210)
            ISOMsg response = new ISOMsg("0210");
            
            // Copy relevant fields from request
            if (pan != null) response.set(2, pan);
            if (processingCode != null) response.set(3, processingCode);
            if (amount != null) response.set(4, amount);
            if (stan != null) response.set(11, stan);
            if (localTime != null) response.set(12, localTime);
            if (localDate != null) response.set(13, localDate);
            
            // Set response specific fields
            response.set(39, responseCode); // Response Code
            response.set(37, generateRRN()); // Retrieval Reference Number
            response.set(38, generateAuthCode()); // Authorization Code (if approved)
            
            // Log outgoing response with masked logging
            maskedLogger.logOutgoingTransaction(response, "POS_TERMINAL");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error handling authorization request", e);
            sendErrorResponse(ctx, msg, "96"); // System Error
        }
    }

    private void handlePosAuthorization(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing POS Authorization Request");
        
        try {
            String processingCode = msg.getString(3);
            String amount = msg.getString(4);
            String pan = msg.getString(2);
            
            // POS-specific processing
            String responseCode = processPosTransaction(processingCode, amount, pan);
            
            ISOMsg response = new ISOMsg("0110"); // POS Authorization Response
            
            // Copy fields and set response
            copyRequestFields(msg, response);
            response.set(39, responseCode);
            response.set(37, generateRRN());
            
            if ("00".equals(responseCode)) {
                response.set(38, generateAuthCode());
            }
            
            // Log outgoing response with masked logging
            maskedLogger.logOutgoingTransaction(response, "POS_TERMINAL");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error handling POS authorization", e);
            sendErrorResponse(ctx, msg, "96");
        }
    }

    private void handleReversalRequest(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing Reversal Request");
        
        try {
            String originalStan = msg.getString(11);
            String originalDate = msg.getString(13);
            String amount = msg.getString(4);
            
            log.info("Reversing transaction - Original STAN: {}, Date: {}, Amount: {}", 
                    originalStan, originalDate, amount);
            
            // Process reversal logic
            boolean reversalSuccessful = processReversal(originalStan, originalDate, amount);
            
            ISOMsg response = new ISOMsg("0410");
            copyRequestFields(msg, response);
            response.set(39, reversalSuccessful ? "00" : "12"); // Approved or Invalid Transaction
            
            // Log outgoing response with masked logging
            maskedLogger.logOutgoingTransaction(response, "POS_TERMINAL");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error handling reversal request", e);
            sendErrorResponse(ctx, msg, "96");
        }
    }

    private void handleAuthorizationAdvice(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing Authorization Advice");
        
        // Advice messages typically don't require a response, just logging/processing
        String stan = msg.getString(11);
        String amount = msg.getString(4);
        
        log.info("Received advice for STAN: {}, Amount: {}", stan, amount);
        
        // Process advice (update records, etc.)
        processAdvice(msg);
    }

    private void handleReversalAdvice(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing Reversal Advice");
        
        String originalStan = msg.getString(11);
        log.info("Received reversal advice for original STAN: {}", originalStan);
        
        // Process reversal advice
        processReversalAdvice(msg);
    }

    private void handleNetworkManagement(ChannelHandlerContext ctx, ISOMsg msg) {
        log.info("Processing Network Management Request");
        
        try {
            String processingCode = msg.getString(3);
            String terminalId = msg.getString(41); // Terminal ID
            String merchantId = msg.getString(42); // Merchant ID (optional)
            
            ISOMsg response = new ISOMsg("0810");
            copyRequestFields(msg, response);
            
            // Network management responses
            switch (processingCode) {
                case "990000": // Sign On
                    response.set(39, "00");
                    log.info("Sign On successful for terminal: {}", terminalId);
                    
                    // Update terminal activity on sign on
                    if (terminalId != null && enableKeyChange) {
                        updateTerminalActivity(terminalId);
                    }
                    break;
                    
                case "990001": // Sign Off
                    response.set(39, "00");
                    log.info("Sign Off successful for terminal: {}", terminalId);
                    break;
                    
                case "990002": // Echo Test
                    response.set(39, "00");
                    log.info("Echo Test successful for terminal: {}", terminalId);
                    break;
                    
                case "900000": // Key Change Request
                    if (enableKeyChange) {
                        handleKeyChangeRequest(msg, response, terminalId, merchantId);
                    } else {
                        log.warn("Key change disabled - rejecting request from terminal: {}", terminalId);
                        response.set(39, "57"); // Transaction not permitted
                    }
                    break;
                    
                case "900001": // Terminal Status Request
                    if (enableKeyChange) {
                        handleTerminalStatusRequest(msg, response, terminalId);
                    } else {
                        response.set(39, "57"); // Transaction not permitted
                    }
                    break;
                    
                default:
                    log.warn("Unknown processing code: {} from terminal: {}", processingCode, terminalId);
                    response.set(39, "12"); // Invalid Transaction
            }
            
            // Log outgoing response with masked logging
            maskedLogger.logOutgoingTransaction(response, "POS_TERMINAL");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error handling network management", e);
            sendErrorResponse(ctx, msg, "96");
        }
    }

    /**
     * Handle key change request from terminal
     * Creates terminal if it doesn't exist and generates new TDES key
     */
    private void handleKeyChangeRequest(ISOMsg request, ISOMsg response, String terminalId, String merchantId) {
        try {
            log.info("Processing key change request for Terminal ID: {}, Merchant ID: {}", terminalId, merchantId);
            
            if (terminalId == null || terminalId.trim().isEmpty()) {
                log.error("Key change request missing Terminal ID");
                response.set(39, "30"); // Format error
                return;
            }

            // Process key change using TerminalManagementService
            TerminalManagementService.KeyChangeResult result = 
                terminalManagementService.processKeyChange(terminalId, merchantId);

            if (result.isSuccess()) {
                // Key change successful
                response.set(39, "00"); // Approved
                
                // Add the new key to response (field 53 - Security Related Control Information)
                // In a real implementation, you might encrypt this or use a different field
                String maskedKey = result.getTerminalKey().getMaskedKeyValue();
                response.set(53, "KEY_ID:" + result.getTerminalKey().getKeyId());
                
                log.info("Key change successful for Terminal {}: Key ID {}, Masked Key: {}", 
                        terminalId, result.getTerminalKey().getKeyId(), maskedKey);
                
                // Log success response
                responseCodeService.logResponseCode("00", "Key change successful", request.getString(11));
                
                maskedLogger.logSystemEvent("KEY_CHANGE_SUCCESS", 
                    String.format("Terminal %s key change completed - Key ID: %s, Masked: %s", 
                                terminalId, result.getTerminalKey().getKeyId(), maskedKey));
                                
            } else {
                // Key change failed
                String responseCode = "96"; // System error
                log.error("Key change failed for Terminal {}: {}", terminalId, result.getMessage());
                response.set(39, responseCode);
                
                // Log detailed error with response code narration
                responseCodeService.logResponseCode(responseCode, 
                    String.format("Key change failed for terminal %s: %s", terminalId, result.getMessage()), 
                    request.getString(11));
                
                maskedLogger.logError("KEY_CHANGE_FAILED", 
                    String.format("Terminal %s key change failed: %s", terminalId, result.getMessage()), null);
            }

        } catch (Exception e) {
            log.error("Error processing key change for Terminal {}: {}", terminalId, e.getMessage(), e);
            response.set(39, "96"); // System error
            
            maskedLogger.logError("KEY_CHANGE_ERROR", 
                String.format("Key change error for Terminal %s", terminalId), e);
        }
    }

    /**
     * Handle terminal status request
     */
    private void handleTerminalStatusRequest(ISOMsg request, ISOMsg response, String terminalId) {
        try {
            log.info("Processing terminal status request for Terminal ID: {}", terminalId);
            
            if (terminalId == null || terminalId.trim().isEmpty()) {
                log.error("Terminal status request missing Terminal ID");
                response.set(39, "30"); // Format error
                return;
            }

            // Get terminal status
            var terminalOpt = terminalManagementService.getTerminal(terminalId);
            
            if (terminalOpt.isPresent()) {
                var terminal = terminalOpt.get();
                response.set(39, "00"); // Approved
                
                // Add terminal status information to response
                String statusInfo = String.format("STATUS:%s,KEYS:%s,CHANGES:%d", 
                    terminal.getStatus().name(),
                    terminal.hasValidKey() ? "VALID" : "INVALID",
                    terminal.getKeyChangeCount());
                    
                response.set(53, statusInfo);
                
                log.info("Terminal status for {}: {}", terminalId, statusInfo);
                
            } else {
                log.warn("Terminal status request for unknown terminal: {}", terminalId);
                response.set(39, "14"); // Invalid card number (terminal not found)
            }

        } catch (Exception e) {
            log.error("Error processing terminal status for Terminal {}: {}", terminalId, e.getMessage(), e);
            response.set(39, "96"); // System error
        }
    }

    /**
     * Update terminal last activity timestamp
     */
    private void updateTerminalActivity(String terminalId) {
        try {
            if (terminalId != null && !terminalId.trim().isEmpty()) {
                var terminalOpt = terminalManagementService.getTerminal(terminalId);
                if (terminalOpt.isPresent()) {
                    var terminal = terminalOpt.get();
                    terminal.updateLastActivity();
                    log.debug("Updated last activity for terminal: {}", terminalId);
                }
            }
        } catch (Exception e) {
            log.warn("Error updating terminal activity for {}: {}", terminalId, e.getMessage());
        }
    }

    private void handleUnsupportedMessage(ChannelHandlerContext ctx, ISOMsg msg) {
        log.warn("Unsupported message type");
        sendErrorResponse(ctx, msg, "12"); // Invalid Transaction
    }

    private void handleError(ChannelHandlerContext ctx, ISOMsg msg, Exception e) {
        log.error("General error handling message", e);
        sendErrorResponse(ctx, msg, "96"); // System Error
    }

    // Business Logic Methods
    private String processTransaction(String processingCode, String amount, String pan) {
        // Sample processing code logic
        if (processingCode == null) return "30"; // Format Error
        
        switch (processingCode) {
            case "000000": // Purchase
                return processPurchase(amount, pan);
            case "010000": // Cash Advance
                return processCashAdvance(amount, pan);
            case "200000": // Refund
                return processRefund(amount, pan);
            case "310000": // Balance Inquiry
                return processBalanceInquiry(pan);
            default:
                log.warn("Unknown processing code: {}", processingCode);
                return "12"; // Invalid Transaction
        }
    }

    private String processPosTransaction(String processingCode, String amount, String pan) {
        // POS-specific transaction processing
        return processTransaction(processingCode, amount, pan);
    }

    private String processPurchase(String amount, String pan) {
        // Sample purchase logic
        if (amount != null && Long.parseLong(amount) > 100000) { // Amount over 1000.00
            return "61"; // Exceeds withdrawal limit
        }
        return "00"; // Approved
    }

    private String processCashAdvance(String amount, String pan) {
        // Sample cash advance logic
        if (amount != null && Long.parseLong(amount) > 50000) { // Amount over 500.00
            return "61"; // Exceeds withdrawal limit
        }
        return "00"; // Approved
    }

    private String processRefund(String amount, String pan) {
        // Sample refund logic
        return "00"; // Approved
    }

    private String processBalanceInquiry(String pan) {
        // Sample balance inquiry logic
        return "00"; // Approved
    }

    private boolean processReversal(String originalStan, String originalDate, String amount) {
        // Sample reversal processing
        log.info("Processing reversal for STAN: {}, Date: {}", originalStan, originalDate);
        return true; // Assume successful
    }

    private void processAdvice(ISOMsg msg) {
        // Process advice message (logging, database updates, etc.)
        log.info("Advice processed");
    }

    private void processReversalAdvice(ISOMsg msg) {
        // Process reversal advice
        log.info("Reversal advice processed");
    }

    // Utility Methods
    private void copyRequestFields(ISOMsg request, ISOMsg response) {
        try {
            // Copy common fields from request to response
            String[] fieldsToCopy = {"2", "3", "4", "11", "12", "13", "14", "22", "25", "37", "41", "42", "43", "49"};
            
            for (String field : fieldsToCopy) {
                if (request.hasField(Integer.parseInt(field))) {
                    response.set(Integer.parseInt(field), request.getString(Integer.parseInt(field)));
                }
            }
        } catch (Exception e) {
            log.error("Error copying request fields", e);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, ISOMsg request, String responseCode) {
        try {
            String requestMti = request.getString(0);
            String responseMti = getResponseMti(requestMti);
            String terminalId = request.getString(41);
            String stan = request.getString(11);
            
            ISOMsg response = new ISOMsg(responseMti);
            copyRequestFields(request, response);
            response.set(39, responseCode);
            
            // Log error response with detailed narration
            String responseMessage = responseCodeService.formatResponseMessage(responseCode, terminalId, stan);
            ResponseCodeService.ResponseCodeInfo codeInfo = responseCodeService.getResponseCodeInfo(responseCode);
            
            // Log based on severity
            switch (codeInfo.getSeverity()) {
                case ERROR:
                    log.error("ERROR RESPONSE: {}", responseMessage);
                    maskedLogger.logError("ERROR_RESPONSE", responseMessage, null);
                    break;
                case WARN:
                    log.warn("WARNING RESPONSE: {}", responseMessage);
                    maskedLogger.logSystemEvent("WARNING_RESPONSE", responseMessage);
                    break;
                default:
                    log.info("RESPONSE: {}", responseMessage);
                    maskedLogger.logSystemEvent("RESPONSE", responseMessage);
                    break;
            }
            
            // Log response code details for analysis
            responseCodeService.logResponseCode(responseCode, 
                String.format("Error response for MTI %s from terminal %s", requestMti, terminalId), 
                stan);
            
            // Log outgoing error response with masked logging
            maskedLogger.logOutgoingTransaction(response, "POS_TERMINAL");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error sending error response", e);
        }
    }

    private String getResponseMti(String requestMti) {
        // Convert request MTI to response MTI
        switch (requestMti) {
            case "0100": return "0110";
            case "0200": return "0210";
            case "0400": return "0410";
            case "0800": return "0810";
            default: return "0210"; // Default to authorization response
        }
    }

    private String generateRRN() {
        // Generate Retrieval Reference Number
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }

    private String generateAuthCode() {
        // Generate Authorization Code
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    /**
     * Process PIN transposition from POS Terminal key to Gateway Zonal key
     * This is called for all incoming transactions that contain PIN data (field 52)
     */
    private void processPinTransposition(ISOMsg msg) {
        try {
            // Check if PIN transposition is enabled
            if (!enablePinTransposition) {
                log.debug("PIN transposition is disabled");
                return;
            }
            
            // Check if message contains PIN data (field 52)
            if (!msg.hasField(52)) {
                log.debug("Message does not contain PIN data (field 52)");
                return;
            }
            
            // Get required fields for PIN transposition
            String encryptedPinBlock = msg.getString(52);
            String pan = msg.getString(2); // Primary Account Number
            String terminalId = msg.getString(41); // Terminal ID
            
            // Validate required fields
            if (pan == null || pan.isEmpty()) {
                log.warn("Cannot transpose PIN: PAN (field 2) is missing");
                maskedLogger.logError("PIN_TRANSPOSE", "PAN missing for PIN transposition", null);
                return;
            }
            
            if (terminalId == null || terminalId.isEmpty()) {
                terminalId = "DEFAULT"; // Use default if terminal ID is missing
                log.debug("Using default terminal ID for PIN transposition");
            }
            
            // Validate PIN block
            if (!pinTranspositionService.validatePinBlock(encryptedPinBlock, pan)) {
                log.warn("Invalid PIN block format, skipping transposition");
                maskedLogger.logError("PIN_TRANSPOSE", "Invalid PIN block format", null);
                return;
            }
            
            log.debug("Starting PIN transposition for terminal: {}", terminalId);
            
            // Transpose PIN from terminal key to gateway zonal key
            String transposedPinBlock = pinTranspositionService.transposePinToGatewayKey(
                encryptedPinBlock, pan, terminalId);
            
            // Update the message with transposed PIN block
            msg.set(52, transposedPinBlock);
            
            log.debug("PIN transposition completed successfully for terminal: {}", terminalId);
            maskedLogger.logSystemEvent("PIN_TRANSPOSE_SUCCESS", 
                String.format("PIN transposed successfully for terminal: %s", terminalId));
            
        } catch (Exception e) {
            log.error("Error during PIN transposition", e);
            maskedLogger.logError("PIN_TRANSPOSE_ERROR", "PIN transposition failed", e);
            
            // For security reasons, we might want to reject the transaction if PIN transposition fails
            // This depends on business requirements
            throw new RuntimeException("PIN transposition failed - transaction cannot proceed", e);
        }
    }
    
    /**
     * Process PIN transposition for outgoing bank messages
     * Converts PIN from Gateway Zonal key to Bank-specific key
     */
    private void processBankPinTransposition(ISOMsg msg, String bankId) {
        try {
            // Check if PIN transposition is enabled
            if (!enablePinTransposition) {
                return;
            }
            
            // Check if message contains PIN data
            if (!msg.hasField(52)) {
                return;
            }
            
            String gatewayPinBlock = msg.getString(52);
            String pan = msg.getString(2);
            
            if (pan == null || pan.isEmpty()) {
                log.warn("Cannot transpose PIN for bank: PAN is missing");
                return;
            }
            
            log.debug("Starting PIN transposition to bank key for bank: {}", bankId);
            
            // Transpose PIN from gateway zonal key to bank key
            String bankPinBlock = pinTranspositionService.transposePinToBankKey(
                gatewayPinBlock, pan, bankId);
            
            // Update message with bank PIN block
            msg.set(52, bankPinBlock);
            
            log.debug("Bank PIN transposition completed for bank: {}", bankId);
            maskedLogger.logSystemEvent("BANK_PIN_TRANSPOSE_SUCCESS", 
                String.format("PIN transposed to bank key for bank: %s", bankId));
            
        } catch (Exception e) {
            log.error("Error during bank PIN transposition for bank: {}", bankId, e);
            maskedLogger.logError("BANK_PIN_TRANSPOSE_ERROR", 
                String.format("Bank PIN transposition failed for bank: %s", bankId), e);
            throw new RuntimeException("Bank PIN transposition failed", e);
        }
    }
    
    /**
     * Validate card number using Luhn algorithm
     * This is called for all incoming transactions to ensure card number validity
     */
    private boolean validateCardNumber(ISOMsg msg) {
        try {
            // Check if card validation is enabled
            if (!enableCardValidation) {
                log.debug("Card validation is disabled");
                return true; // Skip validation if disabled
            }
            
            // Check if message contains PAN (field 2)
            if (!msg.hasField(2)) {
                log.warn("Message does not contain PAN (field 2)");
                maskedLogger.logError("CARD_VALIDATION", "PAN missing for card validation", null);
                return false;
            }
            
            String pan = msg.getString(2);
            String stan = msg.getString(11); // System Trace Audit Number for logging
            String terminalId = msg.getString(41); // Terminal ID
            
            // Create transaction ID for logging
            String transactionId = String.format("%s-%s", 
                terminalId != null ? terminalId : "UNKNOWN", 
                stan != null ? stan : "000000");
            
            log.debug("Validating card number for transaction: {}", transactionId);
            
            // Validate card using Luhn algorithm and card type detection
            boolean isValid = cardValidationService.validateCardForTransaction(pan, transactionId);
            
            if (isValid) {
                log.debug("Card validation successful for transaction: {}", transactionId);
                
                // Get additional card information for enhanced logging
                CardValidationService.CardValidationResult result = cardValidationService.validateCard(pan);
                maskedLogger.logSystemEvent("CARD_VALIDATION_SUCCESS", 
                    String.format("Transaction %s - Card validated: %s (%s)", 
                    transactionId, result.getMaskedPan(), result.getCardType().getDisplayName()));
                
                return true;
            } else {
                log.warn("Card validation failed for transaction: {}", transactionId);
                
                // Get detailed validation result for error logging
                CardValidationService.CardValidationResult result = cardValidationService.validateCard(pan);
                maskedLogger.logError("CARD_VALIDATION_FAILED", 
                    String.format("Transaction %s - Invalid card: %s - %s", 
                    transactionId, result.getMaskedPan(), result.getErrorMessage()), null);
                
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error during card validation", e);
            maskedLogger.logError("CARD_VALIDATION_ERROR", "Card validation processing error", e);
            
            // For security reasons, reject transaction if validation fails due to error
            return false;
        }
    }
    
    /**
     * Validate card number for specific transaction types
     * Some transaction types may have different validation requirements
     */
    private boolean validateCardForTransactionType(ISOMsg msg, String mti) {
        // Basic card validation first
        if (!validateCardNumber(msg)) {
            return false;
        }
        
        try {
            String pan = msg.getString(2);
            CardValidationService.CardValidationResult result = cardValidationService.validateCard(pan);
            
            // Additional validation based on transaction type
            switch (mti) {
                case "0100": // Authorization Request (ATM/POS)
                case "0200": // Financial Request
                    // For financial transactions, ensure card type is supported
                    if (result.getCardType() == CardValidationService.CardType.UNKNOWN) {
                        log.warn("Unsupported card type for financial transaction: {}", result.getMaskedPan());
                        maskedLogger.logError("CARD_TYPE_UNSUPPORTED", 
                            String.format("Unsupported card type for transaction: %s", result.getMaskedPan()), null);
                        return false;
                    }
                    break;
                    
                case "0400": // Reversal Request
                    // For reversals, basic validation is sufficient
                    break;
                    
                case "0800": // Network Management
                    // Network management may not require card validation
                    return true;
                    
                default:
                    // Unknown MTI - apply standard validation
                    break;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error in transaction-specific card validation", e);
            return false;
        }
    }

    // Removed - replaced with MaskedLogger functionality
}
