# ISO 8583 Message Processing Guide

## Overview
This application provides comprehensive ISO 8583 message processing with support for various Message Type Indicators (MTI) and Processing Codes.

## Where to Add Your MTI and Processing Code Logic

### 1. Main Message Handler: `IsoServerHandler.java`
**Location**: `src/main/java/com/kevshake/gateway/cmponents/IsoServerHandler.java`

This is the primary entry point for all incoming ISO 8583 messages. Here you can:

- **Add new MTI handlers** in the main `switch` statement (lines 25-47)
- **Process different message types** with dedicated handler methods
- **Log and validate** incoming messages

#### Supported MTI Types:
- `0100` - POS Authorization Request → Response `0110`
- `0200` - Authorization Request → Response `0210` 
- `0220` - Authorization Advice (no response required)
- `0400` - Reversal Request → Response `0410`
- `0420` - Reversal Advice (no response required)
- `0800` - Network Management Request → Response `0810`

#### Example: Adding a new MTI handler
```java
case "0600": // Administrative Message
    handleAdministrativeMessage(ctx, msg);
    break;
```

### 2. Business Logic Service: `TransactionProcessor.java`
**Location**: `src/main/java/com/kevshake/gateway/cmponents/TransactionProcessor.java`

This service contains the core business logic for processing transactions. Here you can:

- **Add new processing codes** in the `ProcessingCodes` class
- **Implement transaction validation** rules
- **Add custom response codes** in the `ResponseCodes` class
- **Store and retrieve** transaction records

#### Supported Processing Codes:
- `000000` - Purchase Transaction
- `010000` - Cash Advance
- `200000` - Refund Transaction
- `310000` - Balance Inquiry
- `400000` - Payment Transaction
- `500000` - Transfer Transaction
- `990000` - Sign On (Network Management)
- `990001` - Sign Off (Network Management)
- `990002` - Echo Test (Network Management)

#### Example: Adding a new processing code
```java
case "020000": // Void Transaction
    return processVoidTransaction(pan, amount, msg);
```

### 3. Test Client: `Iso8583TestClient.java`
**Location**: `src/main/java/com/kevshake/gateway/cmponents/Iso8583TestClient.java`

Use this client to test your MTI and processing code implementations:

- **Send test messages** to your server
- **Validate responses** and error handling
- **Test different transaction scenarios**

## How to Run and Test

### 1. Start the Server
```bash
mvn spring-boot:run
```
The server will start on:
- **HTTP**: Port 8080 (Tomcat)
- **ISO 8583**: Port 8000 (Netty TCP)

### 2. Run Test Client
```bash
java -cp target/classes com.kevshake.gateway.components.Iso8583TestClient
```

### 3. Send Custom Messages
You can create your own test messages by modifying the test client or using any ISO 8583 testing tool.

## Message Structure Examples

### Authorization Request (0200)
```
MTI: 0200
Field 2: 4111111111111111 (PAN)
Field 3: 000000 (Processing Code - Purchase)
Field 4: 000000010000 (Amount - $100.00)
Field 11: 000001 (STAN)
Field 37: 123456789012 (RRN)
Field 39: 00 (Response Code - Approved)
```

### Network Management (0800)
```
MTI: 0800
Field 3: 990000 (Processing Code - Sign On)
Field 11: 000001 (STAN)
Field 41: TERM0001 (Terminal ID)
Field 42: MERCHANT001 (Merchant ID)
```

## Adding Custom Business Logic

### 1. Transaction Validation
Add validation rules in `TransactionProcessor.java`:

```java
private String validateCustomRules(ISOMsg msg) {
    // Your custom validation logic
    String merchantType = msg.getString(18);
    if ("RESTRICTED".equals(merchantType)) {
        return ResponseCodes.TRANSACTION_NOT_PERMITTED;
    }
    return ResponseCodes.APPROVED;
}
```

### 2. Custom Response Codes
Add new response codes in the `ResponseCodes` class:

```java
public static final String CUSTOM_DECLINE = "99";
public static final String MAINTENANCE_MODE = "98";
```

### 3. Database Integration
Replace the in-memory `transactionDatabase` with actual database calls:

```java
@Autowired
private TransactionRepository transactionRepository;

private void storeTransaction(ISOMsg msg, String responseCode) {
    TransactionEntity entity = new TransactionEntity();
    // Map fields to entity
    transactionRepository.save(entity);
}
```

## Logging and Monitoring

The application provides comprehensive logging:
- **Incoming/Outgoing messages** are fully logged
- **PAN masking** for security
- **Error handling** with detailed stack traces
- **Business logic decisions** are logged

Check logs in the console or configure log files in `application.yml`.

## Security Considerations

- **PAN Masking**: Credit card numbers are automatically masked in logs
- **Field Validation**: All critical fields are validated before processing
- **Error Handling**: Proper error responses prevent information leakage
- **Transaction Limits**: Built-in amount and frequency limits

## Performance Tips

- **Connection Pooling**: Use connection pools for database access
- **Caching**: Cache frequently accessed data (merchant info, card BINs)
- **Async Processing**: Use Spring's `@Async` for non-critical operations
- **Monitoring**: Add metrics and health checks for production use

## Production Checklist

- [ ] Configure proper database connections
- [ ] Set up SSL/TLS for secure communication
- [ ] Implement proper authentication and authorization
- [ ] Add comprehensive monitoring and alerting
- [ ] Configure log rotation and archival
- [ ] Set up backup and disaster recovery
- [ ] Perform load testing and capacity planning
- [ ] Implement proper key management for cryptographic operations
