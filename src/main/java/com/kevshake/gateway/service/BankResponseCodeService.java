package com.kevshake.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Bank Response Code Service
 * Provides bank-side ISO8583 response code descriptions and narrations
 * Specifically for responses coming from the bank/core banking system
 */
@Service
public class BankResponseCodeService {

    private static final Logger logger = LoggerFactory.getLogger(BankResponseCodeService.class);

    // Bank-specific response code descriptions
    private static final Map<String, String> BANK_RESPONSE_CODES = new HashMap<>();
    
    static {
        // Standard success codes
        BANK_RESPONSE_CODES.put("00", "APPROVED - Transaction approved");
        BANK_RESPONSE_CODES.put("10", "PARTIAL APPROVAL - Partial amount approved");
        BANK_RESPONSE_CODES.put("11", "V.I.P. APPROVAL - VIP approval");
        
        // Standard decline codes
        BANK_RESPONSE_CODES.put("01", "Refer to card issuer");
        BANK_RESPONSE_CODES.put("02", "Refer to card issuer - special condition");
        BANK_RESPONSE_CODES.put("03", "Invalid merchant or service provider");
        BANK_RESPONSE_CODES.put("04", "Pickup card");
        BANK_RESPONSE_CODES.put("05", "Do not honor");
        BANK_RESPONSE_CODES.put("06", "Error");
        BANK_RESPONSE_CODES.put("07", "Pickup card, special condition");
        BANK_RESPONSE_CODES.put("12", "Invalid transaction");
        BANK_RESPONSE_CODES.put("13", "Invalid amount");
        BANK_RESPONSE_CODES.put("14", "Invalid account number (no such number)");
        BANK_RESPONSE_CODES.put("15", "No such issuer");
        BANK_RESPONSE_CODES.put("30", "Format error");
        BANK_RESPONSE_CODES.put("41", "Pickup card (lost card)");
        BANK_RESPONSE_CODES.put("43", "Pickup card (stolen card)");
        BANK_RESPONSE_CODES.put("51", "Insufficient funds");
        BANK_RESPONSE_CODES.put("54", "Expired card");
        BANK_RESPONSE_CODES.put("55", "Incorrect PIN");
        BANK_RESPONSE_CODES.put("57", "Transaction not permitted to cardholder");
        
        // Bank-specific response codes from the provided list
        BANK_RESPONSE_CODES.put("58", "Restricted Card - Restricted card");
        BANK_RESPONSE_CODES.put("59", "Insufficient funds - The withdrawal amount exceeds the available account balance");
        BANK_RESPONSE_CODES.put("60", "Uses limit exceeded - The card use limit is exceeded (ATM and POS)");
        BANK_RESPONSE_CODES.put("61", "Withdrawal limit would be exceeded - As a result of the transaction authorization, the withdrawal limit will be exceeded");
        BANK_RESPONSE_CODES.put("62", "PIN tries limit was reached - The invalid PIN tries limit is exceeded");
        BANK_RESPONSE_CODES.put("63", "Withdrawal limit already reached - The withdrawal limit is already reached");
        BANK_RESPONSE_CODES.put("64", "Credit amount limit - Deposit limit is reached");
        BANK_RESPONSE_CODES.put("65", "No statement information - There is no information for account statement");
        BANK_RESPONSE_CODES.put("66", "Statement not available - The Statement request transaction is disabled");
        BANK_RESPONSE_CODES.put("67", "Invalid cash back amount - Invalid cash back amount");
        BANK_RESPONSE_CODES.put("68", "External decline - The transaction was declined by external host");
        BANK_RESPONSE_CODES.put("69", "No sharing - Unmatched request (the card is not serviced in the particular terminal)");
        BANK_RESPONSE_CODES.put("71", "Contact card issuer - Contact card issuer");
        BANK_RESPONSE_CODES.put("72", "Destination not available - The authorization host is not available, for TCI – the side is Offline");
        BANK_RESPONSE_CODES.put("73", "Routing error - Routing error");
        BANK_RESPONSE_CODES.put("74", "Format error - Format error");
        BANK_RESPONSE_CODES.put("75", "External decline special condition - The transaction is declined by the external host following the special condition (cardholder is under suspicion)");
        BANK_RESPONSE_CODES.put("80", "Bad CVV - Bad CVV");
        BANK_RESPONSE_CODES.put("81", "Bad CVV2 - Bad CVV2");
        BANK_RESPONSE_CODES.put("82", "Invalid transaction - Invalid transaction (the transaction with such attributes is prohibited)");
        BANK_RESPONSE_CODES.put("83", "PIN tries limit was exceeded - Bad PIN-code tries limit is already reached (i.e. the bad PIN-code tries limit has been reached and the valid PIN is entered)");
        BANK_RESPONSE_CODES.put("84", "Bad CAVV - Bad 3D Secure Cardholder Authentication Verification Value");
        BANK_RESPONSE_CODES.put("85", "Bad ARQC - Invalid value of the ARQC cryptogram");
        
        // System error codes
        BANK_RESPONSE_CODES.put("90", "SYSTEM ERROR - System malfunction");
        BANK_RESPONSE_CODES.put("91", "Issuer unavailable or switch inoperative");
        BANK_RESPONSE_CODES.put("92", "Destination cannot be found for routing");
        BANK_RESPONSE_CODES.put("93", "Transaction cannot be completed - violation of law");
        BANK_RESPONSE_CODES.put("94", "Duplicate transmission");
        BANK_RESPONSE_CODES.put("95", "Reconciliation failed");
        BANK_RESPONSE_CODES.put("96", "System malfunction - System malfunction or certain field error conditions");
        BANK_RESPONSE_CODES.put("97", "ISSUER/'HSM' OFFLINE - Security module offline");
        BANK_RESPONSE_CODES.put("98", "MAC error");
        BANK_RESPONSE_CODES.put("99", "FATAL ERROR - Fatal system error");
    }

    /**
     * Get bank response code description
     * 
     * @param responseCode the bank response code
     * @return detailed description of the response code
     */
    public String getBankResponseDescription(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return "Unknown bank response code";
        }

        responseCode = responseCode.trim();
        return BANK_RESPONSE_CODES.getOrDefault(responseCode, 
                                              "Unknown bank response code: " + responseCode);
    }

    /**
     * Check if bank response code indicates success
     * 
     * @param responseCode the response code to check
     * @return true if the response indicates success
     */
    public boolean isBankSuccessResponse(String responseCode) {
        return "00".equals(responseCode) || "10".equals(responseCode) || "11".equals(responseCode);
    }

    /**
     * Check if bank response code indicates an error that should be logged
     * 
     * @param responseCode the response code to check
     * @return true if the response indicates an error
     */
    public boolean isBankErrorResponse(String responseCode) {
        return !isBankSuccessResponse(responseCode);
    }

    /**
     * Get bank response severity level for logging purposes
     * 
     * @param responseCode the response code
     * @return severity level (INFO, WARN, ERROR)
     */
    public BankResponseSeverity getBankResponseSeverity(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return BankResponseSeverity.ERROR;
        }

        // Success responses
        if (isBankSuccessResponse(responseCode)) {
            return BankResponseSeverity.INFO;
        }

        // System/Technical errors (high severity)
        if (isBankSystemError(responseCode)) {
            return BankResponseSeverity.ERROR;
        }

        // Security-related errors (high severity)
        if (isBankSecurityError(responseCode)) {
            return BankResponseSeverity.ERROR;
        }

        // Business/User errors (medium severity)
        return BankResponseSeverity.WARN;
    }

    /**
     * Check if bank response code indicates a system/technical error
     * 
     * @param responseCode the response code
     * @return true if it's a system error
     */
    public boolean isBankSystemError(String responseCode) {
        // System error codes that indicate technical issues
        String[] systemErrors = {"06", "72", "73", "74", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};
        
        for (String systemError : systemErrors) {
            if (systemError.equals(responseCode)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if bank response code indicates a security error
     * 
     * @param responseCode the response code
     * @return true if it's a security-related error
     */
    public boolean isBankSecurityError(String responseCode) {
        // Security-related error codes
        String[] securityErrors = {"55", "62", "75", "80", "81", "83", "84", "85"};
        
        for (String securityError : securityErrors) {
            if (securityError.equals(responseCode)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if bank response code indicates insufficient funds
     * 
     * @param responseCode the response code
     * @return true if it's an insufficient funds error
     */
    public boolean isInsufficientFunds(String responseCode) {
        return "51".equals(responseCode) || "59".equals(responseCode);
    }

    /**
     * Check if bank response code indicates limit exceeded
     * 
     * @param responseCode the response code
     * @return true if it's a limit exceeded error
     */
    public boolean isLimitExceeded(String responseCode) {
        String[] limitErrors = {"60", "61", "63", "64"};
        for (String limitError : limitErrors) {
            if (limitError.equals(responseCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log bank response code with appropriate severity and description
     * 
     * @param responseCode the bank response code
     * @param context additional context information
     * @param transactionId transaction identifier for tracking
     */
    public void logBankResponseCode(String responseCode, String context, String transactionId) {
        String description = getBankResponseDescription(responseCode);
        BankResponseSeverity severity = getBankResponseSeverity(responseCode);
        
        String logMessage = String.format("Bank Response Code: %s - %s | Context: %s | Transaction: %s", 
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
     * Create a formatted bank response message for logging
     * 
     * @param responseCode the bank response code
     * @param terminalId terminal ID (optional)
     * @param stan System Trace Audit Number (optional)
     * @return formatted bank response message
     */
    public String formatBankResponseMessage(String responseCode, String terminalId, String stan) {
        String description = getBankResponseDescription(responseCode);
        StringBuilder message = new StringBuilder();
        
        message.append("Bank Response [").append(responseCode).append("]: ").append(description);
        
        if (terminalId != null && !terminalId.trim().isEmpty()) {
            message.append(" | Terminal: ").append(terminalId);
        }
        
        if (stan != null && !stan.trim().isEmpty()) {
            message.append(" | STAN: ").append(stan);
        }
        
        return message.toString();
    }

    /**
     * Get bank response category for classification
     * 
     * @param responseCode the response code
     * @return response category
     */
    public BankResponseCategory getBankResponseCategory(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return BankResponseCategory.UNKNOWN;
        }

        if (isBankSuccessResponse(responseCode)) {
            return BankResponseCategory.SUCCESS;
        }

        if (isBankSystemError(responseCode)) {
            return BankResponseCategory.SYSTEM_ERROR;
        }

        if (isBankSecurityError(responseCode)) {
            return BankResponseCategory.SECURITY_ERROR;
        }

        // Card-related errors
        String[] cardErrors = {"04", "07", "41", "43", "54", "58"};
        for (String cardError : cardErrors) {
            if (cardError.equals(responseCode)) {
                return BankResponseCategory.CARD_ERROR;
            }
        }

        // Account/Funds errors
        if (isInsufficientFunds(responseCode) || isLimitExceeded(responseCode)) {
            return BankResponseCategory.ACCOUNT_ERROR;
        }

        // PIN-related errors
        String[] pinErrors = {"55", "62", "83"};
        for (String pinError : pinErrors) {
            if (pinError.equals(responseCode)) {
                return BankResponseCategory.PIN_ERROR;
            }
        }

        return BankResponseCategory.BUSINESS_ERROR;
    }

    /**
     * Get recommended action based on bank response code
     * 
     * @param responseCode the bank response code
     * @return recommended action for the response code
     */
    public String getRecommendedAction(String responseCode) {
        if (responseCode == null || responseCode.trim().isEmpty()) {
            return "Contact system administrator";
        }

        switch (responseCode) {
            case "00":
            case "10":
            case "11":
                return "Transaction completed successfully";
                
            case "51":
            case "59":
                return "Insufficient funds - Customer should check account balance";
                
            case "55":
                return "Incorrect PIN - Customer should retry with correct PIN";
                
            case "54":
                return "Expired card - Customer should contact card issuer for replacement";
                
            case "58":
                return "Restricted card - Customer should contact card issuer";
                
            case "60":
            case "61":
            case "63":
                return "Transaction limit exceeded - Customer should try smaller amount or contact bank";
                
            case "62":
            case "83":
                return "PIN retry limit exceeded - Customer should contact card issuer";
                
            case "68":
            case "75":
                return "Transaction declined by bank - Customer should contact card issuer";
                
            case "72":
                return "Bank system unavailable - Please try again later";
                
            case "96":
            case "90":
            case "99":
                return "System error - Contact system administrator";
                
            default:
                return "Transaction declined - Customer should contact card issuer";
        }
    }

    // Enums for classification
    public enum BankResponseSeverity {
        INFO, WARN, ERROR
    }

    public enum BankResponseCategory {
        SUCCESS, SYSTEM_ERROR, CARD_ERROR, PIN_ERROR, ACCOUNT_ERROR, 
        SECURITY_ERROR, BUSINESS_ERROR, UNKNOWN
    }

    /**
     * Bank response code information class
     */
    public static class BankResponseCodeInfo {
        private final String code;
        private final String description;
        private final BankResponseSeverity severity;
        private final BankResponseCategory category;
        private final String recommendedAction;

        public BankResponseCodeInfo(String code, String description, BankResponseSeverity severity, 
                                  BankResponseCategory category, String recommendedAction) {
            this.code = code;
            this.description = description;
            this.severity = severity;
            this.category = category;
            this.recommendedAction = recommendedAction;
        }

        // Getters
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public BankResponseSeverity getSeverity() { return severity; }
        public BankResponseCategory getCategory() { return category; }
        public String getRecommendedAction() { return recommendedAction; }

        @Override
        public String toString() {
            return String.format("BankResponseCode{code='%s', description='%s', severity=%s, category=%s, action='%s'}", 
                               code, description, severity, category, recommendedAction);
        }
    }

    /**
     * Get complete bank response code information
     * 
     * @param responseCode the bank response code
     * @return BankResponseCodeInfo object with all details
     */
    public BankResponseCodeInfo getBankResponseCodeInfo(String responseCode) {
        return new BankResponseCodeInfo(
            responseCode,
            getBankResponseDescription(responseCode),
            getBankResponseSeverity(responseCode),
            getBankResponseCategory(responseCode),
            getRecommendedAction(responseCode)
        );
    }

    /**
     * Generate detailed analysis report for bank response
     * 
     * @param responseCode the bank response code
     * @param transactionAmount transaction amount (optional)
     * @param cardType card type (optional)
     * @return detailed analysis report
     */
    public String generateBankResponseAnalysis(String responseCode, String transactionAmount, String cardType) {
        BankResponseCodeInfo info = getBankResponseCodeInfo(responseCode);
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("=== BANK RESPONSE ANALYSIS ===\n");
        analysis.append("Response Code: ").append(info.getCode()).append("\n");
        analysis.append("Description: ").append(info.getDescription()).append("\n");
        analysis.append("Category: ").append(info.getCategory()).append("\n");
        analysis.append("Severity: ").append(info.getSeverity()).append("\n");
        analysis.append("Recommended Action: ").append(info.getRecommendedAction()).append("\n");
        
        if (transactionAmount != null && !transactionAmount.trim().isEmpty()) {
            analysis.append("Transaction Amount: ").append(transactionAmount).append("\n");
        }
        
        if (cardType != null && !cardType.trim().isEmpty()) {
            analysis.append("Card Type: ").append(cardType).append("\n");
        }
        
        // Add specific insights based on response code
        if (isInsufficientFunds(responseCode)) {
            analysis.append("INSIGHT: This is a funds-related decline. Customer may need to check account balance or transfer funds.\n");
        } else if (isLimitExceeded(responseCode)) {
            analysis.append("INSIGHT: Transaction exceeds configured limits. Customer may need to contact bank to adjust limits.\n");
        } else if (isBankSecurityError(responseCode)) {
            analysis.append("INSIGHT: Security-related error detected. This may indicate potential fraud or authentication issues.\n");
        } else if (isBankSystemError(responseCode)) {
            analysis.append("INSIGHT: System-level error. This may require technical investigation or system maintenance.\n");
        }
        
        analysis.append("===============================");
        
        return analysis.toString();
    }
}
