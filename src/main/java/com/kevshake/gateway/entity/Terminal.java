package com.kevshake.gateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Terminal Entity - Manages POS terminals and their associated keys
 * Each terminal is identified by TID (Terminal ID) from ISO8583 field 41
 */
@Entity
@Table(name = "terminals", 
       uniqueConstraints = {@UniqueConstraint(columnNames = "terminal_id")})
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Terminal ID cannot be blank")
    @Size(min = 8, max = 16, message = "Terminal ID must be between 8 and 16 characters")
    @Column(name = "terminal_id", nullable = false, unique = true, length = 16)
    private String terminalId; // TID from ISO8583 field 41

    @Size(max = 15, message = "Merchant ID cannot exceed 15 characters")
    @Column(name = "merchant_id", length = 15)
    private String merchantId; // From ISO8583 field 42

    @Size(max = 100, message = "Terminal name cannot exceed 100 characters")
    @Column(name = "terminal_name", length = 100)
    private String terminalName;

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    @Column(name = "location")
    private String location;

    @NotNull(message = "Terminal status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TerminalStatus status = TerminalStatus.ACTIVE;

    @Size(max = 20, message = "Terminal type cannot exceed 20 characters")
    @Column(name = "terminal_type", length = 20)
    private String terminalType = "POS"; // POS, ATM, etc.

    @Size(max = 50, message = "Serial number cannot exceed 50 characters")
    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Size(max = 20, message = "Software version cannot exceed 20 characters")
    @Column(name = "software_version", length = 20)
    private String softwareVersion;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;

    @Column(name = "key_change_count")
    private Integer keyChangeCount = 0;

    @Column(name = "last_key_change_date")
    private LocalDateTime lastKeyChangeDate;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    @Column(name = "notes")
    private String notes;

    // One-to-One relationship with TerminalKey
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", referencedColumnName = "keyId")
    private TerminalKey terminalKey;

    // Constructors
    public Terminal() {}
    public Terminal(String terminalId) {
        this.terminalId = terminalId;
        this.status = TerminalStatus.ACTIVE;
        this.keyChangeCount = 0;
    }
    public Terminal(String terminalId, String merchantId) {
        this(terminalId);
        this.merchantId = merchantId;
    }

    // Terminal Status Enum
    public enum TerminalStatus {
        ACTIVE("Active - Ready for transactions"),
        INACTIVE("Inactive - Not processing transactions"),
        SUSPENDED("Suspended - Temporarily disabled"),
        MAINTENANCE("Maintenance - Under service"),
        DECOMMISSIONED("Decommissioned - Permanently disabled");

        private final String description;
        TerminalStatus(String description) {
            this.description = description;
        }

    /** Returns the description. */
        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    /** Sets the id. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Returns the terminal id. */
    public String getTerminalId() {
        return terminalId;
    }

    /** Sets the terminal id. */
    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    /** Returns the merchant id. */
    public String getMerchantId() {
        return merchantId;
    }

    /** Sets the merchant id. */
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    /** Returns the terminal name. */
    public String getTerminalName() {
        return terminalName;
    }

    /** Sets the terminal name. */
    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    /** Returns the location. */
    public String getLocation() {
        return location;
    }

    /** Sets the location. */
    public void setLocation(String location) {
        this.location = location;
    }

    /** Returns the status. */
    public TerminalStatus getStatus() {
        return status;
    }

    /** Sets the status. */
    public void setStatus(TerminalStatus status) {
        this.status = status;
    }

    /** Returns the terminal type. */
    public String getTerminalType() {
        return terminalType;
    }

    /** Sets the terminal type. */
    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    /** Returns the serial number. */
    public String getSerialNumber() {
        return serialNumber;
    }

    /** Sets the serial number. */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /** Returns the software version. */
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    /** Sets the software version. */
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    /** Returns the created date. */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /** Sets the created date. */
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /** Returns the updated date. */
    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    /** Sets the updated date. */
    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    /** Returns the last activity date. */
    public LocalDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    /** Sets the last activity date. */
    public void setLastActivityDate(LocalDateTime lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    /** Returns the key change count. */
    public Integer getKeyChangeCount() {
        return keyChangeCount;
    }

    /** Sets the key change count. */
    public void setKeyChangeCount(Integer keyChangeCount) {
        this.keyChangeCount = keyChangeCount;
    }

    /** Returns the last key change date. */
    public LocalDateTime getLastKeyChangeDate() {
        return lastKeyChangeDate;
    }

    /** Sets the last key change date. */
    public void setLastKeyChangeDate(LocalDateTime lastKeyChangeDate) {
        this.lastKeyChangeDate = lastKeyChangeDate;
    }

    /** Returns the notes. */
    public String getNotes() {
        return notes;
    }

    /** Sets the notes. */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** Returns the terminal key. */
    public TerminalKey getTerminalKey() {
        return terminalKey;
    }

    /** Sets the terminal key. */
    public void setTerminalKey(TerminalKey terminalKey) {
        this.terminalKey = terminalKey;
        if (terminalKey != null) {
            terminalKey.setTerminal(this);
        }
    }

    // Business Methods
    public boolean isActive() {
        return status == TerminalStatus.ACTIVE;
    }
    /** Implements canProcessTransactions for Terminal. */
    public boolean canProcessTransactions() {
        return status == TerminalStatus.ACTIVE && hasValidKey();
    }
    /** Implements hasValidKey for Terminal. */
    public boolean hasValidKey() {
        return terminalKey != null && terminalKey.isActive() && !terminalKey.isExpired();
    }
    /** Implements updateLastActivity for Terminal. */
    public void updateLastActivity() {
        this.lastActivityDate = LocalDateTime.now();
    }
    /** Implements incrementKeyChangeCount for Terminal. */
    public void incrementKeyChangeCount() {
        this.keyChangeCount = (this.keyChangeCount == null) ? 1 : this.keyChangeCount + 1;
        this.lastKeyChangeDate = LocalDateTime.now();
    }
    /** Implements activate for Terminal. */
    public void activate() {
        this.status = TerminalStatus.ACTIVE;
    }
    /** Implements deactivate for Terminal. */
    public void deactivate() {
        this.status = TerminalStatus.INACTIVE;
    }
    /** Implements suspend for Terminal. */
    public void suspend() {
        this.status = TerminalStatus.SUSPENDED;
    }

    /** Sets the maintenance. */
    public void setMaintenance() {
        this.status = TerminalStatus.MAINTENANCE;
    }
    /** Implements decommission for Terminal. */
    public void decommission() {
        this.status = TerminalStatus.DECOMMISSIONED;
        if (terminalKey != null) {
            terminalKey.deactivate();
        }
    }

    // Utility Methods
    public String getDisplayName() {
        if (terminalName != null && !terminalName.trim().isEmpty()) {
            return terminalName + " (" + terminalId + ")";
        }
        return terminalId;
    }

    /** Returns the key info. */
    public String getKeyInfo() {
        if (terminalKey == null) {
            return "No key assigned";
        }
        return "Key ID: " + terminalKey.getKeyId() + 
               ", Status: " + terminalKey.getKeyStatus() + 
               ", Masked: " + terminalKey.getMaskedKeyValue();
    }

    @Override
    /** Implements equals for Terminal. */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return Objects.equals(terminalId, terminal.terminalId);
    }

    @Override
    /** Implements hashCode for Terminal. */
    public int hashCode() {
        return Objects.hash(terminalId);
    }

    @Override
    /** Implements toString for Terminal. */
    public String toString() {
        return "Terminal{" +
                "id=" + id +
                ", terminalId='" + terminalId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", terminalName='" + terminalName + '\'' +
                ", status=" + status +
                ", keyChangeCount=" + keyChangeCount +
                ", hasKey=" + (terminalKey != null) +
                ", createdDate=" + createdDate +
                '}';
    }
}
