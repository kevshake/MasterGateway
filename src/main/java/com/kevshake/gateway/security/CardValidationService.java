package com.kevshake.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevshake.gateway.components.MaskedLogger;

/**
 * Card Validation Service using Luhn Algorithm
 * Validates PAN/Card numbers for mathematical correctness
 * 
 * The Luhn algorithm is used to validate credit card numbers, IMEI numbers, and other identifier numbers.
 * It was designed to protect against accidental errors in manually entered numbers.
 */
@Service
public class CardValidationService {
    private static final Logger log = LoggerFactory.getLogger(CardValidationService.class);
    
    @Autowired
    private MaskedLogger maskedLogger;
    
    /**
     * Card type enumeration based on card number patterns
     */
    public enum CardType {
        VISA("Visa", "^4[0-9]{12}(?:[0-9]{3})?$"),
        MASTERCARD("Mastercard", "^5[1-5][0-9]{14}$|^2(?:2(?:2[1-9]|[3-9][0-9])|[3-6][0-9][0-9]|7(?:[01][0-9]|20))[0-9]{12}$"),
        AMERICAN_EXPRESS("American Express", "^3[47][0-9]{13}$"),
        DISCOVER("Discover", "^6(?:011|5[0-9]{2})[0-9]{12}$"),
        JCB("JCB", "^(?:2131|1800|35\\d{3})\\d{11}$"),
        DINERS_CLUB("Diners Club", "^3(?:0[0-5]|[68][0-9])[0-9]{11}$"),
        MAESTRO("Maestro", "^(?:5[0678]\\d\\d|6304|6390|67\\d\\d)\\d{8,15}$"),
        UNKNOWN("Unknown", ".*");
        
        private final String displayName;
        private final String pattern;
        
        CardType(String displayName, String pattern) {
            this.displayName = displayName;
            this.pattern = pattern;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getPattern() {
            return pattern;
        }
    }
    
    /**
     * Card validation result class
     */
    public static class CardValidationResult {
        private final boolean valid;
        private final boolean luhnValid;
        private final CardType cardType;
        private final String maskedPan;
        private final String errorMessage;
        
        public CardValidationResult(boolean valid, boolean luhnValid, CardType cardType, String maskedPan, String errorMessage) {
            this.valid = valid;
            this.luhnValid = luhnValid;
            this.cardType = cardType;
            this.maskedPan = maskedPan;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public boolean isLuhnValid() { return luhnValid; }
        public CardType getCardType() { return cardType; }
        public String getMaskedPan() { return maskedPan; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return String.format("CardValidationResult{valid=%s, luhnValid=%s, cardType=%s, maskedPan='%s', errorMessage='%s'}", 
                valid, luhnValid, cardType.getDisplayName(), maskedPan, errorMessage);
        }
    }
    
    /**
     * Validate card number using Luhn algorithm and card type detection
     * 
     * @param pan Primary Account Number (card number)
     * @return CardValidationResult with validation details
     */
    public CardValidationResult validateCard(String pan) {
        try {
            // Basic format validation
            if (pan == null || pan.trim().isEmpty()) {
                return new CardValidationResult(false, false, CardType.UNKNOWN, "****", "PAN is null or empty");
            }
            
            // Remove any spaces or non-digit characters
            String cleanPan = pan.replaceAll("[^0-9]", "");
            
            // Check minimum and maximum length
            if (cleanPan.length() < 13 || cleanPan.length() > 19) {
                String masked = maskPan(cleanPan);
                return new CardValidationResult(false, false, CardType.UNKNOWN, masked, 
                    String.format("Invalid PAN length: %d (must be 13-19 digits)", cleanPan.length()));
            }
            
            // Detect card type
            CardType cardType = detectCardType(cleanPan);
            String maskedPan = maskPan(cleanPan);
            
            // Perform Luhn validation
            boolean luhnValid = isLuhnValid(cleanPan);
            
            // Overall validation (Luhn + format)
            boolean valid = luhnValid && cardType != CardType.UNKNOWN;
            
            // Log validation attempt (without exposing sensitive data)
            if (valid) {
                maskedLogger.logSystemEvent("CARD_VALIDATION_SUCCESS", 
                    String.format("Card validation successful - Type: %s, Masked PAN: %s", 
                    cardType.getDisplayName(), maskedPan));
            } else {
                maskedLogger.logSystemEvent("CARD_VALIDATION_FAILED", 
                    String.format("Card validation failed - Type: %s, Masked PAN: %s, Luhn Valid: %s", 
                    cardType.getDisplayName(), maskedPan, luhnValid));
            }
            
            String errorMessage = null;
            if (!luhnValid) {
                errorMessage = "Luhn algorithm validation failed";
            } else if (cardType == CardType.UNKNOWN) {
                errorMessage = "Unknown or unsupported card type";
            }
            
            return new CardValidationResult(valid, luhnValid, cardType, maskedPan, errorMessage);
            
        } catch (Exception e) {
            log.error("Error during card validation", e);
            maskedLogger.logError("CARD_VALIDATION_ERROR", "Card validation processing error", e);
            return new CardValidationResult(false, false, CardType.UNKNOWN, "****", "Validation processing error");
        }
    }
    
    /**
     * Validate card number using only Luhn algorithm (fast validation)
     * 
     * @param pan Primary Account Number
     * @return true if Luhn algorithm passes, false otherwise
     */
    public boolean isValidLuhn(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return false;
        }
        
        String cleanPan = pan.replaceAll("[^0-9]", "");
        return isLuhnValid(cleanPan);
    }
    
    /**
     * Detect card type based on card number patterns
     * 
     * @param pan Clean card number (digits only)
     * @return CardType enum value
     */
    public CardType detectCardType(String pan) {
        if (pan == null || pan.isEmpty()) {
            return CardType.UNKNOWN;
        }
        
        // Check each card type pattern
        for (CardType cardType : CardType.values()) {
            if (cardType != CardType.UNKNOWN && pan.matches(cardType.getPattern())) {
                return cardType;
            }
        }
        
        return CardType.UNKNOWN;
    }
    
    /**
     * Implementation of the Luhn algorithm
     * 
     * The Luhn algorithm:
     * 1. Starting from the rightmost digit (excluding check digit) and moving left,
     *    double the value of every second digit
     * 2. If the result of this doubling operation is greater than 9, 
     *    add the digits of the result
     * 3. Take the sum of all the digits
     * 4. If the sum modulo 10 equals 0, then the number is valid
     * 
     * @param pan Clean card number (digits only)
     * @return true if Luhn validation passes
     */
    private boolean isLuhnValid(String pan) {
        if (pan == null || pan.length() < 2) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = pan.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(pan.charAt(i));
            
            if (digit < 0 || digit > 9) {
                return false; // Invalid character
            }
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1; // Add digits: 10->1, 11->2, ..., 18->9
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
    
    /**
     * Mask PAN for logging and display purposes
     * Shows first 4 and last 4 digits, masks the middle
     * 
     * @param pan Card number to mask
     * @return Masked card number
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        
        if (pan.length() <= 8) {
            return pan.substring(0, 4) + "****";
        }
        
        return pan.substring(0, 4) + 
               "*".repeat(pan.length() - 8) + 
               pan.substring(pan.length() - 4);
    }
    
    /**
     * Validate card number and log the result
     * This method is used in transaction processing
     * 
     * @param pan Primary Account Number
     * @param transactionId Transaction identifier for logging
     * @return true if card is valid, false otherwise
     */
    public boolean validateCardForTransaction(String pan, String transactionId) {
        CardValidationResult result = validateCard(pan);
        
        // Log transaction-specific validation
        if (result.isValid()) {
            maskedLogger.logSystemEvent("TXN_CARD_VALIDATION_SUCCESS", 
                String.format("Transaction %s - Card validation successful: %s (%s)", 
                transactionId, result.getMaskedPan(), result.getCardType().getDisplayName()));
        } else {
            maskedLogger.logSystemEvent("TXN_CARD_VALIDATION_FAILED", 
                String.format("Transaction %s - Card validation failed: %s - %s", 
                transactionId, result.getMaskedPan(), result.getErrorMessage()));
        }
        
        return result.isValid();
    }
    
    /**
     * Get detailed card information for administrative purposes
     * 
     * @param pan Primary Account Number
     * @return Detailed card information string
     */
    public String getCardInfo(String pan) {
        CardValidationResult result = validateCard(pan);
        
        StringBuilder info = new StringBuilder();
        info.append(String.format("Card Information:\n"));
        info.append(String.format("  Masked PAN: %s\n", result.getMaskedPan()));
        info.append(String.format("  Card Type: %s\n", result.getCardType().getDisplayName()));
        info.append(String.format("  Luhn Valid: %s\n", result.isLuhnValid()));
        info.append(String.format("  Overall Valid: %s\n", result.isValid()));
        
        if (result.getErrorMessage() != null) {
            info.append(String.format("  Error: %s\n", result.getErrorMessage()));
        }
        
        return info.toString();
    }
    
    /**
     * Batch validate multiple card numbers
     * 
     * @param pans Array of card numbers to validate
     * @return Array of validation results
     */
    public CardValidationResult[] validateCards(String[] pans) {
        if (pans == null) {
            return new CardValidationResult[0];
        }
        
        CardValidationResult[] results = new CardValidationResult[pans.length];
        
        for (int i = 0; i < pans.length; i++) {
            results[i] = validateCard(pans[i]);
        }
        
        return results;
    }
    
    /**
     * Test method to demonstrate Luhn algorithm with known test cases
     */
    public void testLuhnAlgorithm() {
        log.info("=== Luhn Algorithm Test Cases ===");
        
        // Test cases: [PAN, Expected Result, Description]
        Object[][] testCases = {
            {"4532015112830366", true, "Visa test card"},
            {"4000000000000002", true, "Visa test card"},
            {"5555555555554444", true, "Mastercard test card"},
            {"5105105105105100", true, "Mastercard test card"},
            {"378282246310005", true, "American Express test card"},
            {"371449635398431", true, "American Express test card"},
            {"6011111111111117", true, "Discover test card"},
            {"30569309025904", true, "Diners Club test card"},
            {"3530111333300000", true, "JCB test card"},
            {"4532015112830367", false, "Invalid Visa (wrong check digit)"},
            {"1234567890123456", false, "Invalid number"},
            {"", false, "Empty string"},
            {null, false, "Null value"}
        };
        
        int passed = 0;
        int total = testCases.length;
        
        for (Object[] testCase : testCases) {
            String pan = (String) testCase[0];
            boolean expected = (Boolean) testCase[1];
            String description = (String) testCase[2];
            
            CardValidationResult result = validateCard(pan);
            boolean actual = result.isLuhnValid();
            
            if (actual == expected) {
                passed++;
                log.info("✓ PASS: {} - {} ({})", description, result.getMaskedPan(), result.getCardType().getDisplayName());
            } else {
                log.error("✗ FAIL: {} - Expected: {}, Actual: {} ({})", description, expected, actual, result.getMaskedPan());
            }
        }
        
        log.info("=== Test Results: {}/{} passed ===", passed, total);
        
        if (passed == total) {
            maskedLogger.logSystemEvent("LUHN_TEST_SUCCESS", "All Luhn algorithm tests passed");
        } else {
            maskedLogger.logSystemEvent("LUHN_TEST_FAILED", 
                String.format("Luhn algorithm tests failed: %d/%d passed", passed, total));
        }
    }
    
    /**
     * Generate a valid test card number for a specific card type (for testing only)
     * 
     * @param cardType Card type to generate
     * @param length Desired length (within valid range for card type)
     * @return Valid test card number
     */
    public String generateTestCardNumber(CardType cardType, int length) {
        // This is for testing purposes only - never use in production for real card generation
        
        String prefix;
        int minLength, maxLength;
        
        switch (cardType) {
            case VISA:
                prefix = "4";
                minLength = 13;
                maxLength = 19;
                break;
            case MASTERCARD:
                prefix = "5555";
                minLength = 16;
                maxLength = 16;
                break;
            case AMERICAN_EXPRESS:
                prefix = "34";
                minLength = 15;
                maxLength = 15;
                break;
            default:
                prefix = "4000";
                minLength = 16;
                maxLength = 16;
                break;
        }
        
        // Validate length
        if (length < minLength || length > maxLength) {
            length = minLength;
        }
        
        // Generate random digits for the middle part
        StringBuilder cardNumber = new StringBuilder(prefix);
        while (cardNumber.length() < length - 1) {
            cardNumber.append((int) (Math.random() * 10));
        }
        
        // Calculate and append Luhn check digit
        int checkDigit = calculateLuhnCheckDigit(cardNumber.toString());
        cardNumber.append(checkDigit);
        
        return cardNumber.toString();
    }
    
    /**
     * Calculate Luhn check digit for a partial card number
     * 
     * @param partialCardNumber Card number without check digit
     * @return Check digit (0-9)
     */
    private int calculateLuhnCheckDigit(String partialCardNumber) {
        int sum = 0;
        boolean alternate = true; // Start with true because we're calculating for the check digit position
        
        for (int i = partialCardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(partialCardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (10 - (sum % 10)) % 10;
    }
}
