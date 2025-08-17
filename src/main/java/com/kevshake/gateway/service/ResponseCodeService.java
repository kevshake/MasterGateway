package com.kevshake.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Response Code Service
 * Provides ISO8583 response code descriptions and narrations
 * Based on standard ISO8583 response codes with custom extensions
 */
@Service
public class ResponseCodeService {

    private static final Logger logger = LoggerFactory.getLogger(ResponseCodeService.class);

    // Response code descriptions array (index-based for numeric codes)
    private static final String[] RESPONSE_DESCRIPTIONS = {
        /* 00*/ "APPROVED",
        /* 01*/ "Refer to card issuer",
        /* 02*/ "Refer to card issuer - special condition",
        /* 03*/ "Invalid merchant or service provider",
        /* 04*/ "Pickup card",
        /* 05*/ "Do not honor",
        /* 06*/ "Error",
        /* 07*/ "Pickup card, special condition (other than lost/stolen card)",
        /* 08*/ "",
        /* 09*/ "",
        /* 10*/ "Partial Approval",
        /* 11*/ "V.I.P. approval",
        /* 12*/ "Invalid transaction",
        /* 13*/ "Invalid amount (currency conversion field overflow) or amount exceeds maximum for card program",
        /* 14*/ "Invalid account number (no such number)",
        /* 15*/ "No such issuer",
        /* 16*/ "",
        /* 17*/ "",
        /* 18*/ "Account number does not exist",
        /* 19*/ "Re-enter transaction",
        /* 20*/ "",
        /* 21*/ "No action taken (unable to back out prior transaction)",
        /* 22*/ "",
        /* 23*/ "",
        /* 24*/ "",
        /* 25*/ "Unable to locate record in file, or account number is missing from the inquiry",
        /* 26*/ "",
        /* 27*/ "",
        /* 28*/ "File is temporarily unavailable",
        /* 29*/ "",
        /* 30*/ "Format error",
        /* 31*/ "",
        /* 32*/ "",
        /* 33*/ "CARD ERROR",
        /* 34*/ "USER DOESNT EXIST",
        /* 35*/ "",
        /* 36*/ "USER STATUS LOCKED",
        /* 37*/ "",
        /* 38*/ "",
        /* 39*/ "",
        /* 40*/ "",
        /* 41*/ "Pickup card (lost card)",
        /* 42*/ "ACCOUNT LOCKED",
        /* 43*/ "Pickup card (stolen card)",
        /* 44*/ "",
        /* 45*/ "SYSTEM ERROR",   //for Equity
        /* 46*/ "User already Exists",
        /* 47*/ "Query Failed",
        /* 48*/ "NO validation",
        /* 49*/ "POS version not supported",
        /* 50*/ "",
        /* 51*/ "Insufficient funds",
        /* 52*/ "No checking account",
        /* 53*/ "No savings account",
        /* 54*/ "Expired card",
        /* 55*/ "Incorrect PIN",
        /* 56*/ "NO CARD RECORD",
        /* 57*/ "Transaction not permitted to cardholder",
        /* 58*/ "Transaction not allowed at terminal",
        /* 59*/ "Suspected fraud",
        /* 60*/ "",
        /* 61*/ "Activity amount limit exceeded",
        /* 62*/ "Restricted card (for example, in Country Exclusion table)",
        /* 63*/ "Security violation",
        /* 64*/ "No User Found",
        /* 65*/ "Activity count limit exceeded",
        /* 66*/ "Wrong User ID",
        /* 67*/ "Wrong PIN",
        /* 68*/ "Wrong User Role",
        /* 69*/ "TERMINAL ID ERROR",
        /* 70*/ "User access denied",
        /* 71*/ "User(ID) does not exist",
        /* 72*/ "Wrong PIN(password)",
        /* 73*/ "Wrong User Role",
        /* 74*/ "Operator not tied to the agent",
        /* 75*/ "Allowable number of PIN-entry tries exceeded",
        /* 76*/ "Unable to locate previous message (no match on Retrieval Reference number)",
        /* 77*/ "Previous message located for a repeat or reversal, but repeat or reversal data are inconsistent with original message",
        /* 78*/ "Blocked, first used The transaction is from a new cardholder, and the card has not been properly unblocked.",
        /* 79*/ "",
        /* 80*/ "Visa transactions: credit issuer unavailable. Private label and check acceptance: Invalid date",
        /* 81*/ "PIN cryptographic error found (error found by VIC security module during PIN decryption)",
        /* 82*/ "Negative CAM, dCVV, iCVV, or CVV results",
        /* 83*/ "Unable to verify PIN",
        /* 84*/ "",
        /* 85*/ "No reason to decline a request for account number verification, address verification, CVV2 verification, or a credit voucher or merchandise return",
        /* 86*/ "",
        /* 87*/ "",
        /* 88*/ "",
        /* 89*/ "",
        /* 90*/ "SYSTEM ERROR",
        /* 91*/ "Issuer unavailable or switch inoperative (STIP not applicable or available for this transaction)",
        /* 92*/ "Destination cannot be found for routing",
        /* 93*/ "Transaction cannot be completed - violation of law",
        /* 94*/ "",
        /* 95*/ "Reconciliation failed",
        /* 96*/ "System malfunction, System malfunction or certain field error conditions",
        /* 97*/ "ISSUER/'HSM' OFFLINE",
        /* 98*/ "",
        /* 99*/ "FATAL ERROR"
    };

    // Additional non-numeric response codes
    private static final Map<String, String> ALPHA_RESPONSE_CODES = new HashMap<>();
    
    static {
        ALPHA_RESPONSE_CODES.put("B1", "Surcharge amount not permitted on Visa cards (U.S. acquirers only)");
        ALPHA_RESPONSE_CODES.put("N0", "Force STIP");
        ALPHA_RESPONSE_CODES.put("N3", "Cash service not available");
        ALPHA_RESPONSE_CODES.put("N4", "Cashback request exceeds issuer limit");
        ALPHA_RESPONSE_CODES.put("N7", "Decline for CVV2 failure");
        ALPHA_RESPONSE_CODES.put("P2", "Invalid biller information");
        ALPHA_RESPONSE_CODES.put("P5", "PIN Change/Unblock request declined");
        ALPHA_RESPONSE_CODES.put("P6", "Unsafe PIN");
        ALPHA_RESPONSE_CODES.put("Q1", "Card Authentication failed");
        ALPHA_RESPONSE_CODES.put("R0", "Stop Payment Order");
        ALPHA_RESPONSE_CODES.put("R1", "Revocation of Authorization Order");
        ALPHA_RESPONSE_CODES.put("R3", "Revocation of All Authorizations Order");
        ALPHA_RESPONSE_CODES.put("XA", "Forward to issuer");
        ALPHA_RESPONSE_CODES.put("XD", "Forward to issuer");
        ALPHA_RESPONSE_CODES.put("Z3", "Unable to go online");
    }

    /**
     * Get response code description by response code string
     * 
     * @param responseCode the response code (e.g., "00", "14", "96", "B1")
     * @return description of the response code
     */
    public String getResponseDescription(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return "Unknown response code";
        }

        responseCode = responseCode.trim();

        try {
            // Try to parse as numeric response code
            int code = Integer.parseInt(responseCode);
            return getResponseDescription(code);
        } catch (NumberFormatException e) {
            // Handle alpha-numeric response codes
            return ALPHA_RESPONSE_CODES.getOrDefault(responseCode.toUpperCase(), 
                                                   "Unknown response code: " + responseCode);
        }
    }

    /**
     * Get response code description by numeric response code
     * 
     * @param responseCode the numeric response code (0-99)
     * @return description of the response code
     */
    public String getResponseDescription(int responseCode) {
        if (responseCode < 0 || responseCode >= RESPONSE_DESCRIPTIONS.length) {
            return "Invalid response code: " + responseCode;
        }

        String description = RESPONSE_DESCRIPTIONS[responseCode];
        if (description == null || description.trim().isEmpty()) {
            return "Reserved/Unused response code: " + String.format("%02d", responseCode);
        }

        return description;
    }

    /**
     * Check if response code indicates success
     * 
     * @param responseCode the response code to check
     * @return true if the response indicates success
     */
    public boolean isSuccessResponse(String responseCode) {
        return "00".equals(responseCode) || "10".equals(responseCode) || "11".equals(responseCode);
    }

    /**
     * Check if response code indicates an error that should be logged
     * 
     * @param responseCode the response code to check
     * @return true if the response indicates an error
     */
    public boolean isErrorResponse(String responseCode) {
        return !isSuccessResponse(responseCode);
    }

    /**
     * Get response severity level for logging purposes
     * 
     * @param responseCode the response code
     * @return severity level (INFO, WARN, ERROR)
     */
    public ResponseSeverity getResponseSeverity(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return ResponseSeverity.ERROR;
        }

        // Success responses
        if (isSuccessResponse(responseCode)) {
            return ResponseSeverity.INFO;
        }

        // System/Technical errors (high severity)
        if (isSystemError(responseCode)) {
            return ResponseSeverity.ERROR;
        }

        // Business/User errors (medium severity)
        return ResponseSeverity.WARN;
    }

    /**
     * Check if response code indicates a system/technical error
     * 
     * @param responseCode the response code
     * @return true if it's a system error
     */
    public boolean isSystemError(String responseCode) {
        // System error codes that indicate technical issues
        String[] systemErrors = {"06", "28", "45", "90", "91", "92", "96", "97", "99"};
        
        for (String systemError : systemErrors) {
            if (systemError.equals(responseCode)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Log response code with appropriate severity and description
     * 
     * @param responseCode the response code
     * @param context additional context information
     * @param transactionId transaction identifier for tracking
     */
    public void logResponseCode(String responseCode, String context, String transactionId) {
        String description = getResponseDescription(responseCode);
        ResponseSeverity severity = getResponseSeverity(responseCode);
        
        String logMessage = String.format("Response Code: %s - %s | Context: %s | Transaction: %s", 
                                        responseCode, description, context, transactionId);

        switch (severity) {
            case INFO:
                logger.info(logMessage);
                break;
            case WARN:
                logger.warn(logMessage);
                break;
            case ERROR:
                logger.error(logMessage);
                break;
        }
    }

    /**
     * Create a formatted response message for logging
     * 
     * @param responseCode the response code
     * @param terminalId terminal ID (optional)
     * @param stan System Trace Audit Number (optional)
     * @return formatted response message
     */
    public String formatResponseMessage(String responseCode, String terminalId, String stan) {
        String description = getResponseDescription(responseCode);
        StringBuilder message = new StringBuilder();
        
        message.append("Response [").append(responseCode).append("]: ").append(description);
        
        if (terminalId != null && !terminalId.trim().isEmpty()) {
            message.append(" | Terminal: ").append(terminalId);
        }
        
        if (stan != null && !stan.trim().isEmpty()) {
            message.append(" | STAN: ").append(stan);
        }
        
        return message.toString();
    }

    /**
     * Get response category for classification
     * 
     * @param responseCode the response code
     * @return response category
     */
    public ResponseCategory getResponseCategory(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return ResponseCategory.UNKNOWN;
        }

        if (isSuccessResponse(responseCode)) {
            return ResponseCategory.SUCCESS;
        }

        if (isSystemError(responseCode)) {
            return ResponseCategory.SYSTEM_ERROR;
        }

        // Card-related errors
        String[] cardErrors = {"04", "07", "33", "41", "43", "54", "56", "62"};
        for (String cardError : cardErrors) {
            if (cardError.equals(responseCode)) {
                return ResponseCategory.CARD_ERROR;
            }
        }

        // PIN-related errors
        String[] pinErrors = {"55", "67", "72", "75", "81", "83"};
        for (String pinError : pinErrors) {
            if (pinError.equals(responseCode)) {
                return ResponseCategory.PIN_ERROR;
            }
        }

        // Account-related errors
        String[] accountErrors = {"14", "18", "51", "52", "53", "61", "65"};
        for (String accountError : accountErrors) {
            if (accountError.equals(responseCode)) {
                return ResponseCategory.ACCOUNT_ERROR;
            }
        }

        // Security-related errors
        String[] securityErrors = {"59", "63", "78", "82"};
        for (String securityError : securityErrors) {
            if (securityError.equals(responseCode)) {
                return ResponseCategory.SECURITY_ERROR;
            }
        }

        return ResponseCategory.BUSINESS_ERROR;
    }

    // Enums for classification
    public enum ResponseSeverity {
        INFO, WARN, ERROR
    }

    public enum ResponseCategory {
        SUCCESS, SYSTEM_ERROR, CARD_ERROR, PIN_ERROR, ACCOUNT_ERROR, 
        SECURITY_ERROR, BUSINESS_ERROR, UNKNOWN
    }

    /**
     * Response code information class
     */
    public static class ResponseCodeInfo {
        private final String code;
        private final String description;
        private final ResponseSeverity severity;
        private final ResponseCategory category;

        public ResponseCodeInfo(String code, String description, ResponseSeverity severity, ResponseCategory category) {
            this.code = code;
            this.description = description;
            this.severity = severity;
            this.category = category;
        }

        // Getters
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public ResponseSeverity getSeverity() { return severity; }
        public ResponseCategory getCategory() { return category; }

        @Override
        public String toString() {
            return String.format("ResponseCode{code='%s', description='%s', severity=%s, category=%s}", 
                               code, description, severity, category);
        }
    }

    /**
     * Get complete response code information
     * 
     * @param responseCode the response code
     * @return ResponseCodeInfo object with all details
     */
    public ResponseCodeInfo getResponseCodeInfo(String responseCode) {
        return new ResponseCodeInfo(
            responseCode,
            getResponseDescription(responseCode),
            getResponseSeverity(responseCode),
            getResponseCategory(responseCode)
        );
    }
}
