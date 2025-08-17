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

        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public TerminalStatus getStatus() {
        return status;
    }

    public void setStatus(TerminalStatus status) {
        this.status = status;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public LocalDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDateTime lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Integer getKeyChangeCount() {
        return keyChangeCount;
    }

    public void setKeyChangeCount(Integer keyChangeCount) {
        this.keyChangeCount = keyChangeCount;
    }

    public LocalDateTime getLastKeyChangeDate() {
        return lastKeyChangeDate;
    }

    public void setLastKeyChangeDate(LocalDateTime lastKeyChangeDate) {
        this.lastKeyChangeDate = lastKeyChangeDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public TerminalKey getTerminalKey() {
        return terminalKey;
    }

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

    public boolean canProcessTransactions() {
        return status == TerminalStatus.ACTIVE && hasValidKey();
    }

    public boolean hasValidKey() {
        return terminalKey != null && terminalKey.isActive() && !terminalKey.isExpired();
    }

    public void updateLastActivity() {
        this.lastActivityDate = LocalDateTime.now();
    }

    public void incrementKeyChangeCount() {
        this.keyChangeCount = (this.keyChangeCount == null) ? 1 : this.keyChangeCount + 1;
        this.lastKeyChangeDate = LocalDateTime.now();
    }

    public void activate() {
        this.status = TerminalStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = TerminalStatus.INACTIVE;
    }

    public void suspend() {
        this.status = TerminalStatus.SUSPENDED;
    }

    public void setMaintenance() {
        this.status = TerminalStatus.MAINTENANCE;
    }

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

    public String getKeyInfo() {
        if (terminalKey == null) {
            return "No key assigned";
        }
        return "Key ID: " + terminalKey.getKeyId() + 
               ", Status: " + terminalKey.getKeyStatus() + 
               ", Masked: " + terminalKey.getMaskedKeyValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return Objects.equals(terminalId, terminal.terminalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminalId);
    }

    @Override
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
