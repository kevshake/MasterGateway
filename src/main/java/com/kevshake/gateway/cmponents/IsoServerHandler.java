package com.kevshake.gateway.cmponents;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Handler: Process messages
public class IsoServerHandler extends SimpleChannelInboundHandler<ISOMsg> {
    private static final Logger log = LoggerFactory.getLogger(IsoServerHandler.class);
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ISOMsg msg) {
        try {
            // Get MTI (Message Type Indicator)
            String mti = msg.getString(0);
            log.info("Received message with MTI: {}", mti);
            
            // Log incoming message fields
            logMessageFields(msg, "INCOMING");
            
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
            
            logMessageFields(response, "OUTGOING");
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
            
            logMessageFields(response, "OUTGOING");
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
            
            logMessageFields(response, "OUTGOING");
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
            
            ISOMsg response = new ISOMsg("0810");
            copyRequestFields(msg, response);
            
            // Network management responses
            switch (processingCode) {
                case "990000": // Sign On
                    response.set(39, "00");
                    log.info("Sign On successful");
                    break;
                case "990001": // Sign Off
                    response.set(39, "00");
                    log.info("Sign Off successful");
                    break;
                case "990002": // Echo Test
                    response.set(39, "00");
                    log.info("Echo Test successful");
                    break;
                default:
                    response.set(39, "12"); // Invalid Transaction
            }
            
            logMessageFields(response, "OUTGOING");
            ctx.writeAndFlush(response);
            
        } catch (Exception e) {
            log.error("Error handling network management", e);
            sendErrorResponse(ctx, msg, "96");
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
            
            ISOMsg response = new ISOMsg(responseMti);
            copyRequestFields(request, response);
            response.set(39, responseCode);
            
            logMessageFields(response, "ERROR_RESPONSE");
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

    private void logMessageFields(ISOMsg msg, String direction) {
        try {
            log.info("=== {} MESSAGE ===", direction);
            log.info("MTI: {}", msg.getString(0));
            
            // Log key fields
            for (int i = 1; i <= 128; i++) {
                if (msg.hasField(i)) {
                    String value = msg.getString(i);
                    if (i == 2) value = maskPan(value); // Mask PAN
                    log.info("Field {}: {}", i, value);
                }
            }
            log.info("=== END {} MESSAGE ===", direction);
        } catch (Exception e) {
            log.error("Error logging message fields", e);
        }
    }
}
