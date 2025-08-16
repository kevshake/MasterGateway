package com.kevshake.gateway.cmponents;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.ISO87APackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample test client to send ISO 8583 messages to the server
 * This is useful for testing your MTI and processing code implementations
 */
public class Iso8583TestClient {
    private static final Logger log = LoggerFactory.getLogger(Iso8583TestClient.class);
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8000;
    
    private ISOPackager packager;
    
    public Iso8583TestClient() {
        this.packager = new ISO87APackager();
    }
    
    /**
     * Send a test authorization request (0200)
     */
    public void sendAuthorizationRequest() {
        try {
            ISOMsg msg = new ISOMsg("0200");
            
            // Set required fields
            msg.set(2, "4111111111111111");    // PAN
            msg.set(3, "000000");              // Processing Code (Purchase)
            msg.set(4, "000000010000");        // Amount ($100.00)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());       // System Trace Audit Number
            msg.set(12, getCurrentTime());     // Local Transaction Time
            msg.set(13, getCurrentDate());     // Local Transaction Date
            msg.set(14, "2512");               // Expiration Date
            msg.set(22, "051");                // POS Entry Mode
            msg.set(25, "00");                 // POS Condition Code
            msg.set(37, generateRRN());        // Retrieval Reference Number
            msg.set(41, "TERM0001");           // Terminal ID
            msg.set(42, "MERCHANT001");        // Merchant ID
            msg.set(43, "TEST MERCHANT    ANYTOWN    US");
            msg.set(49, "840");                // Currency Code (USD)
            
            sendMessage(msg, "Authorization Request");
            
        } catch (Exception e) {
            log.error("Error sending authorization request", e);
        }
    }
    
    /**
     * Send a test POS authorization request (0100)
     */
    public void sendPosAuthorizationRequest() {
        try {
            ISOMsg msg = new ISOMsg("0100");
            
            msg.set(2, "5555555555554444");    // PAN
            msg.set(3, "000000");              // Processing Code (Purchase)
            msg.set(4, "000000005000");        // Amount ($50.00)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            msg.set(12, getCurrentTime());
            msg.set(13, getCurrentDate());
            msg.set(14, "2512");
            msg.set(22, "051");
            msg.set(25, "00");
            msg.set(37, generateRRN());
            msg.set(41, "POSTERM01");
            msg.set(42, "POSMERCH001");
            msg.set(43, "POS TEST MERCHANT ANYTOWN    US");
            msg.set(49, "840");
            
            sendMessage(msg, "POS Authorization Request");
            
        } catch (Exception e) {
            log.error("Error sending POS authorization request", e);
        }
    }
    
    /**
     * Send a test cash advance request
     */
    public void sendCashAdvanceRequest() {
        try {
            ISOMsg msg = new ISOMsg("0200");
            
            msg.set(2, "4000000000000002");
            msg.set(3, "010000");              // Processing Code (Cash Advance)
            msg.set(4, "000000030000");        // Amount ($300.00)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            msg.set(12, getCurrentTime());
            msg.set(13, getCurrentDate());
            msg.set(14, "2512");
            msg.set(22, "051");
            msg.set(25, "00");
            msg.set(37, generateRRN());
            msg.set(41, "ATM00001");
            msg.set(42, "BANKMERCH01");
            msg.set(43, "ATM LOCATION     ANYTOWN    US");
            msg.set(49, "840");
            
            sendMessage(msg, "Cash Advance Request");
            
        } catch (Exception e) {
            log.error("Error sending cash advance request", e);
        }
    }
    
    /**
     * Send a test balance inquiry
     */
    public void sendBalanceInquiry() {
        try {
            ISOMsg msg = new ISOMsg("0200");
            
            msg.set(2, "4111111111111111");
            msg.set(3, "310000");              // Processing Code (Balance Inquiry)
            msg.set(4, "000000000000");        // Amount (0 for balance inquiry)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            msg.set(12, getCurrentTime());
            msg.set(13, getCurrentDate());
            msg.set(14, "2512");
            msg.set(22, "051");
            msg.set(25, "00");
            msg.set(37, generateRRN());
            msg.set(41, "ATM00001");
            msg.set(42, "BANKMERCH01");
            msg.set(43, "ATM LOCATION     ANYTOWN    US");
            msg.set(49, "840");
            
            sendMessage(msg, "Balance Inquiry");
            
        } catch (Exception e) {
            log.error("Error sending balance inquiry", e);
        }
    }
    
    /**
     * Send a test reversal request
     */
    public void sendReversalRequest() {
        try {
            ISOMsg msg = new ISOMsg("0400");
            
            msg.set(2, "4111111111111111");
            msg.set(3, "000000");              // Processing Code (Purchase Reversal)
            msg.set(4, "000000010000");        // Original Amount
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, "000001");             // Original STAN
            msg.set(12, getCurrentTime());
            msg.set(13, getCurrentDate());     // Original Date
            msg.set(37, "123456789012");       // Original RRN
            msg.set(41, "TERM0001");
            msg.set(42, "MERCHANT001");
            msg.set(43, "TEST MERCHANT    ANYTOWN    US");
            msg.set(49, "840");
            msg.set(90, "020011234567890123456789012345678901234567890123"); // Original Data Elements
            
            sendMessage(msg, "Reversal Request");
            
        } catch (Exception e) {
            log.error("Error sending reversal request", e);
        }
    }
    
    /**
     * Send network management requests
     */
    public void sendSignOnRequest() {
        try {
            ISOMsg msg = new ISOMsg("0800");
            
            msg.set(3, "990000");              // Processing Code (Sign On)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            msg.set(41, "TERM0001");
            msg.set(42, "MERCHANT001");
            
            sendMessage(msg, "Sign On Request");
            
        } catch (Exception e) {
            log.error("Error sending sign on request", e);
        }
    }
    
    public void sendEchoTest() {
        try {
            ISOMsg msg = new ISOMsg("0800");
            
            msg.set(3, "990002");              // Processing Code (Echo Test)
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            msg.set(41, "TERM0001");
            msg.set(42, "MERCHANT001");
            
            sendMessage(msg, "Echo Test");
            
        } catch (Exception e) {
            log.error("Error sending echo test", e);
        }
    }
    
    /**
     * Send a message that will cause an error (for testing error handling)
     */
    public void sendInvalidMessage() {
        try {
            ISOMsg msg = new ISOMsg("0200");
            
            // Invalid PAN (too short)
            msg.set(2, "411111");
            msg.set(3, "999999");              // Invalid Processing Code
            msg.set(4, "000000010000");
            msg.set(7, getCurrentTransmissionDateTime());
            msg.set(11, generateSTAN());
            
            sendMessage(msg, "Invalid Message (Test Error Handling)");
            
        } catch (Exception e) {
            log.error("Error sending invalid message", e);
        }
    }
    
    private void sendMessage(ISOMsg msg, String description) {
        Socket socket = null;
        try {
            log.info("Sending {}: {}", description, msg.getString(0));
            
            // Log outgoing message
            logMessage(msg, "OUTGOING " + description);
            
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Pack and send message
            byte[] data = msg.pack();
            byte[] lengthBytes = new byte[2];
            lengthBytes[0] = (byte) ((data.length >> 8) & 0xFF);
            lengthBytes[1] = (byte) (data.length & 0xFF);
            
            out.write(lengthBytes);
            out.write(data);
            out.flush();
            
            // Read response
            byte[] responseLength = new byte[2];
            in.read(responseLength);
            int length = ((responseLength[0] & 0xFF) << 8) | (responseLength[1] & 0xFF);
            
            byte[] responseData = new byte[length];
            in.read(responseData);
            
            // Unpack response
            ISOMsg response = new ISOMsg();
            response.unpack(responseData);
            
            log.info("Received response: {} - Response Code: {}", 
                    response.getString(0), response.getString(39));
            
            logMessage(response, "INCOMING RESPONSE");
            
        } catch (Exception e) {
            log.error("Error communicating with server", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
            }
        }
    }
    
    private void logMessage(ISOMsg msg, String direction) {
        try {
            log.info("=== {} ===", direction);
            log.info("MTI: {}", msg.getString(0));
            
            for (int i = 1; i <= 128; i++) {
                if (msg.hasField(i)) {
                    String value = msg.getString(i);
                    if (i == 2) value = maskPan(value); // Mask PAN
                    log.info("Field {}: {}", i, value);
                }
            }
            log.info("=== END {} ===", direction);
        } catch (Exception e) {
            log.error("Error logging message", e);
        }
    }
    
    private String getCurrentTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }
    
    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }
    
    private String generateSTAN() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
    
    private String generateRRN() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
    
    /**
     * Main method to run test scenarios
     */
    public static void main(String[] args) {
        Iso8583TestClient client = new Iso8583TestClient();
        
        log.info("Starting ISO 8583 test client...");
        
        try {
            // Wait a bit for server to be ready
            Thread.sleep(2000);
            
            // Test different message types
            log.info("=== Testing Network Management ===");
            client.sendSignOnRequest();
            Thread.sleep(1000);
            
            client.sendEchoTest();
            Thread.sleep(1000);
            
            log.info("=== Testing Financial Transactions ===");
            client.sendAuthorizationRequest();
            Thread.sleep(1000);
            
            client.sendPosAuthorizationRequest();
            Thread.sleep(1000);
            
            client.sendCashAdvanceRequest();
            Thread.sleep(1000);
            
            client.sendBalanceInquiry();
            Thread.sleep(1000);
            
            log.info("=== Testing Reversal ===");
            client.sendReversalRequest();
            Thread.sleep(1000);
            
            log.info("=== Testing Error Handling ===");
            client.sendInvalidMessage();
            Thread.sleep(1000);
            
            log.info("Test client completed successfully!");
            
        } catch (Exception e) {
            log.error("Error running test client", e);
        }
    }
}
