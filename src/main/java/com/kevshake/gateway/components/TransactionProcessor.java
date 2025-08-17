package com.kevshake.gateway.components;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessor {
    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);
    
    // Sample transaction database (in real application, use actual database)
    private Map<String, TransactionRecord> transactionDatabase = new HashMap<>();
    
    // Processing Code Constants
    public static class ProcessingCodes {
        public static final String PURCHASE = "000000";
        public static final String CASH_ADVANCE = "010000";
        public static final String REFUND = "200000";
        public static final String BALANCE_INQUIRY = "310000";
        public static final String PAYMENT = "400000";
        public static final String TRANSFER = "500000";
        
        // Network Management
        public static final String SIGN_ON = "990000";
        public static final String SIGN_OFF = "990001";
        public static final String ECHO_TEST = "990002";
    }
    
    // Response Code Constants
    public static class ResponseCodes {
        public static final String APPROVED = "00";
        public static final String INVALID_TRANSACTION = "12";
        public static final String INVALID_AMOUNT = "13";
        public static final String INVALID_CARD = "14";
        public static final String NO_SUCH_ISSUER = "15";
        public static final String FORMAT_ERROR = "30";
        public static final String PICKUP_CARD = "33";
        public static final String RESTRICTED_CARD = "36";
        public static final String CALL_ACQUIRER = "37";
        public static final String PICKUP_CARD_SPECIAL = "38";
        public static final String DECLINED = "51";
        public static final String EXPIRED_CARD = "54";
        public static final String INCORRECT_PIN = "55";
        public static final String TRANSACTION_NOT_PERMITTED = "57";
        public static final String EXCEEDS_LIMIT = "61";
        public static final String RESTRICTED_CARD_COUNTRY = "62";
        public static final String SECURITY_VIOLATION = "63";
        public static final String EXCEEDS_FREQUENCY_LIMIT = "65";
        public static final String ALLOWABLE_PIN_TRIES_EXCEEDED = "75";
        public static final String INVALID_ACCOUNT = "76";
        public static final String CRYPTOGRAPHIC_ERROR = "81";
        public static final String TIMEOUT = "91";
        public static final String DUPLICATE_TRANSMISSION = "94";
        public static final String SYSTEM_ERROR = "96";
        public static final String COMMS_ERROR = "98";
    }
    
    /**
     * Process a financial transaction based on processing code
     */
    public String processFinancialTransaction(ISOMsg msg) {
        try {
            String processingCode = msg.getString(3);
            String pan = msg.getString(2);
            String amount = msg.getString(4);
            String stan = msg.getString(11);
            
            log.info("Processing financial transaction - Code: {}, PAN: {}, Amount: {}, STAN: {}", 
                    processingCode, maskPan(pan), amount, stan);
            
            // Validate basic fields
            if (processingCode == null || processingCode.length() != 6) {
                return ResponseCodes.FORMAT_ERROR;
            }
            
            if (pan == null || pan.length() < 13 || pan.length() > 19) {
                return ResponseCodes.INVALID_CARD;
            }
            
            // Check for duplicate transaction
            if (isDuplicateTransaction(stan, msg.getString(13))) {
                return ResponseCodes.DUPLICATE_TRANSMISSION;
            }
            
            // Process based on transaction type
            String responseCode = processTransactionByType(processingCode, pan, amount, msg);
            
            // Store transaction record
            if (ResponseCodes.APPROVED.equals(responseCode)) {
                storeTransaction(msg, responseCode);
            }
            
            return responseCode;
            
        } catch (Exception e) {
            log.error("Error processing financial transaction", e);
            return ResponseCodes.SYSTEM_ERROR;
        }
    }
    
    private String processTransactionByType(String processingCode, String pan, String amount, ISOMsg msg) {
        switch (processingCode) {
            case ProcessingCodes.PURCHASE:
                return processPurchaseTransaction(pan, amount, msg);
                
            case ProcessingCodes.CASH_ADVANCE:
                return processCashAdvanceTransaction(pan, amount, msg);
                
            case ProcessingCodes.REFUND:
                return processRefundTransaction(pan, amount, msg);
                
            case ProcessingCodes.BALANCE_INQUIRY:
                return processBalanceInquiryTransaction(pan, msg);
                
            case ProcessingCodes.PAYMENT:
                return processPaymentTransaction(pan, amount, msg);
                
            case ProcessingCodes.TRANSFER:
                return processTransferTransaction(pan, amount, msg);
                
            default:
                log.warn("Unsupported processing code: {}", processingCode);
                return ResponseCodes.INVALID_TRANSACTION;
        }
    }
    
    private String processPurchaseTransaction(String pan, String amount, ISOMsg msg) {
        log.info("Processing purchase transaction");
        
        // Sample business rules for purchase
        if (amount == null) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        long amountValue = Long.parseLong(amount);
        
        // Check amount limits
        if (amountValue <= 0) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        if (amountValue > 500000) { // Max $5000
            return ResponseCodes.EXCEEDS_LIMIT;
        }
        
        // Check card validity (sample logic)
        if (!isValidCard(pan)) {
            return ResponseCodes.INVALID_CARD;
        }
        
        // Check account balance (sample logic)
        if (!hasSufficientBalance(pan, amountValue)) {
            return ResponseCodes.DECLINED;
        }
        
        // Check merchant category restrictions
        String merchantCategory = msg.getString(18);
        if (isRestrictedMerchant(merchantCategory)) {
            return ResponseCodes.TRANSACTION_NOT_PERMITTED;
        }
        
        return ResponseCodes.APPROVED;
    }
    
    private String processCashAdvanceTransaction(String pan, String amount, ISOMsg msg) {
        log.info("Processing cash advance transaction");
        
        if (amount == null) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        long amountValue = Long.parseLong(amount);
        
        // Cash advance has lower limits
        if (amountValue > 100000) { // Max $1000 for cash advance
            return ResponseCodes.EXCEEDS_LIMIT;
        }
        
        if (!isValidCard(pan)) {
            return ResponseCodes.INVALID_CARD;
        }
        
        if (!hasCashAdvanceLimit(pan, amountValue)) {
            return ResponseCodes.EXCEEDS_LIMIT;
        }
        
        return ResponseCodes.APPROVED;
    }
    
    private String processRefundTransaction(String pan, String amount, ISOMsg msg) {
        log.info("Processing refund transaction");
        
        // Refunds typically have different validation rules
        if (amount == null) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        // Check if original transaction exists
        String originalRRN = msg.getString(37);
        if (originalRRN != null && !hasOriginalTransaction(originalRRN)) {
            return ResponseCodes.INVALID_TRANSACTION;
        }
        
        return ResponseCodes.APPROVED;
    }
    
    private String processBalanceInquiryTransaction(String pan, ISOMsg msg) {
        log.info("Processing balance inquiry transaction");
        
        if (!isValidCard(pan)) {
            return ResponseCodes.INVALID_CARD;
        }
        
        // Balance inquiry is usually approved if card is valid
        return ResponseCodes.APPROVED;
    }
    
    private String processPaymentTransaction(String pan, String amount, ISOMsg msg) {
        log.info("Processing payment transaction");
        
        if (amount == null) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        long amountValue = Long.parseLong(amount);
        
        if (amountValue <= 0) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        if (!isValidCard(pan)) {
            return ResponseCodes.INVALID_CARD;
        }
        
        return ResponseCodes.APPROVED;
    }
    
    private String processTransferTransaction(String pan, String amount, ISOMsg msg) {
        log.info("Processing transfer transaction");
        
        if (amount == null) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        long amountValue = Long.parseLong(amount);
        
        if (amountValue <= 0) {
            return ResponseCodes.INVALID_AMOUNT;
        }
        
        if (amountValue > 1000000) { // Max $10,000 for transfers
            return ResponseCodes.EXCEEDS_LIMIT;
        }
        
        if (!isValidCard(pan)) {
            return ResponseCodes.INVALID_CARD;
        }
        
        // Check if destination account exists (from field 103)
        String destinationAccount = msg.getString(103);
        if (destinationAccount != null && !isValidDestinationAccount(destinationAccount)) {
            return ResponseCodes.INVALID_ACCOUNT;
        }
        
        return ResponseCodes.APPROVED;
    }
    
    /**
     * Process network management requests
     */
    public String processNetworkManagement(ISOMsg msg) {
        try {
            String processingCode = msg.getString(3);
            
            switch (processingCode) {
                case ProcessingCodes.SIGN_ON:
                    return processSignOn(msg);
                    
                case ProcessingCodes.SIGN_OFF:
                    return processSignOff(msg);
                    
                case ProcessingCodes.ECHO_TEST:
                    return processEchoTest(msg);
                    
                default:
                    return ResponseCodes.INVALID_TRANSACTION;
            }
            
        } catch (Exception e) {
            log.error("Error processing network management request", e);
            return ResponseCodes.SYSTEM_ERROR;
        }
    }
    
    private String processSignOn(ISOMsg msg) {
        log.info("Processing sign-on request");
        
        String terminalId = msg.getString(41);
        String merchantId = msg.getString(42);
        
        log.info("Terminal {} signing on for merchant {}", terminalId, merchantId);
        
        // In real implementation, validate terminal and merchant
        return ResponseCodes.APPROVED;
    }
    
    private String processSignOff(ISOMsg msg) {
        log.info("Processing sign-off request");
        
        String terminalId = msg.getString(41);
        log.info("Terminal {} signing off", terminalId);
        
        return ResponseCodes.APPROVED;
    }
    
    private String processEchoTest(ISOMsg msg) {
        log.info("Processing echo test");
        return ResponseCodes.APPROVED;
    }
    
    /**
     * Process reversal transactions
     */
    public String processReversal(ISOMsg msg) {
        try {
            String originalStan = msg.getString(11);
            String originalDate = msg.getString(13);
            String amount = msg.getString(4);
            
            log.info("Processing reversal - Original STAN: {}, Date: {}, Amount: {}", 
                    originalStan, originalDate, amount);
            
            // Find original transaction
            TransactionRecord originalTransaction = findOriginalTransaction(originalStan, originalDate);
            
            if (originalTransaction == null) {
                log.warn("Original transaction not found for reversal");
                return ResponseCodes.INVALID_TRANSACTION;
            }
            
            // Process the reversal
            boolean reversalProcessed = reverseTransaction(originalTransaction);
            
            return reversalProcessed ? ResponseCodes.APPROVED : ResponseCodes.SYSTEM_ERROR;
            
        } catch (Exception e) {
            log.error("Error processing reversal", e);
            return ResponseCodes.SYSTEM_ERROR;
        }
    }
    
    // Utility and validation methods
    private boolean isValidCard(String pan) {
        // Sample card validation logic
        if (pan == null || pan.length() < 13) {
            return false;
        }
        
        // Simple Luhn algorithm check (for demo purposes)
        return pan.startsWith("4") || pan.startsWith("5") || pan.startsWith("6"); // Visa, MC, Discover
    }
    
    private boolean hasSufficientBalance(String pan, long amount) {
        // Sample balance check logic
        // In real implementation, check against account balance
        return amount <= 200000; // Sample limit of $2000
    }
    
    private boolean hasCashAdvanceLimit(String pan, long amount) {
        // Sample cash advance limit check
        return amount <= 50000; // Sample limit of $500
    }
    
    private boolean isRestrictedMerchant(String merchantCategory) {
        // Sample merchant restriction logic
        if (merchantCategory == null) return false;
        
        // Block gambling merchants (MCC 7995)
        return "7995".equals(merchantCategory);
    }
    
    private boolean isValidDestinationAccount(String account) {
        // Sample destination account validation
        return account != null && account.length() >= 10;
    }
    
    private boolean isDuplicateTransaction(String stan, String date) {
        String key = stan + "_" + date;
        return transactionDatabase.containsKey(key);
    }
    
    private boolean hasOriginalTransaction(String rrn) {
        return transactionDatabase.values().stream()
                .anyMatch(t -> rrn.equals(t.getRrn()));
    }
    
    private void storeTransaction(ISOMsg msg, String responseCode) {
        try {
            String key = msg.getString(11) + "_" + msg.getString(13);
            TransactionRecord record = new TransactionRecord();
            record.setStan(msg.getString(11));
            record.setDate(msg.getString(13));
            record.setTime(msg.getString(12));
            record.setPan(msg.getString(2));
            record.setAmount(msg.getString(4));
            record.setProcessingCode(msg.getString(3));
            record.setResponseCode(responseCode);
            record.setRrn(generateRRN());
            record.setTimestamp(LocalDateTime.now());
            
            transactionDatabase.put(key, record);
            log.info("Transaction stored: {}", key);
            
        } catch (Exception e) {
            log.error("Error storing transaction", e);
        }
    }
    
    private TransactionRecord findOriginalTransaction(String stan, String date) {
        String key = stan + "_" + date;
        return transactionDatabase.get(key);
    }
    
    private boolean reverseTransaction(TransactionRecord original) {
        // Sample reversal logic
        log.info("Reversing transaction: STAN={}, Amount={}", 
                original.getStan(), original.getAmount());
        
        // Mark as reversed
        original.setReversed(true);
        
        return true;
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    private String generateRRN() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
    
    // Inner class for transaction records
    public static class TransactionRecord {
        private String stan;
        private String date;
        private String time;
        private String pan;
        private String amount;
        private String processingCode;
        private String responseCode;
        private String rrn;
        private LocalDateTime timestamp;
        private boolean reversed = false;
        
        // Getters and setters
        public String getStan() { return stan; }
        public void setStan(String stan) { this.stan = stan; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getProcessingCode() { return processingCode; }
        public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }
        
        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
        
        public String getRrn() { return rrn; }
        public void setRrn(String rrn) { this.rrn = rrn; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isReversed() { return reversed; }
        public void setReversed(boolean reversed) { this.reversed = reversed; }
    }
}
