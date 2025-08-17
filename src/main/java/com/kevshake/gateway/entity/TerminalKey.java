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
 * TerminalKey Entity - Manages TDES keys for terminals
 * Each key is associated with a terminal and contains the encrypted key data
 */
@Entity
@Table(name = "terminal_keys")
public class TerminalKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long keyId;

    @NotBlank(message = "Key value cannot be blank")
    @Size(min = 32, max = 48, message = "Key value must be between 32 and 48 characters")
    @Column(name = "key_value", nullable = false, length = 48)
    private String keyValue;

    @NotBlank(message = "Key type cannot be blank")
    @Column(name = "key_type", nullable = false, length = 20)
    private String keyType = "TDES"; // Default to TDES

    @NotNull(message = "Key status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "key_status", nullable = false)
    private KeyStatus keyStatus = KeyStatus.ACTIVE;

    @Size(max = 16, message = "Key Check Value cannot exceed 16 characters")
    @Column(name = "key_check_value", length = 16)
    private String keyCheckValue; // KCV for key verification

    @Column(name = "key_length")
    private Integer keyLength; // 2 for double-length, 3 for triple-length

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    @Column(name = "notes")
    private String notes;

    // One-to-One relationship with Terminal (mapped by terminal)
    @OneToOne(mappedBy = "terminalKey", fetch = FetchType.LAZY)
    private Terminal terminal;

    // Constructors
    public TerminalKey() {}

    public TerminalKey(String keyValue, String keyType, Integer keyLength) {
        this.keyValue = keyValue;
        this.keyType = keyType;
        this.keyLength = keyLength;
        this.keyStatus = KeyStatus.ACTIVE;
    }

    // Key Status Enum
    public enum KeyStatus {
        ACTIVE("Active - Ready for use"),
        INACTIVE("Inactive - Not in use"),
        EXPIRED("Expired - Past expiry date"),
        COMPROMISED("Compromised - Security breach"),
        PENDING("Pending - Awaiting activation");

        private final String description;

        KeyStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public Long getKeyId() {
        return keyId;
    }

    public void setKeyId(Long keyId) {
        this.keyId = keyId;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public KeyStatus getKeyStatus() {
        return keyStatus;
    }

    public void setKeyStatus(KeyStatus keyStatus) {
        this.keyStatus = keyStatus;
    }

    public String getKeyCheckValue() {
        return keyCheckValue;
    }

    public void setKeyCheckValue(String keyCheckValue) {
        this.keyCheckValue = keyCheckValue;
    }

    public Integer getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(Integer keyLength) {
        this.keyLength = keyLength;
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

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    // Business Methods
    public boolean isActive() {
        return keyStatus == KeyStatus.ACTIVE;
    }

    public boolean isExpired() {
        return keyStatus == KeyStatus.EXPIRED || 
               (expiryDate != null && expiryDate.isBefore(LocalDateTime.now()));
    }

    public void activate() {
        this.keyStatus = KeyStatus.ACTIVE;
    }

    public void deactivate() {
        this.keyStatus = KeyStatus.INACTIVE;
    }

    public void expire() {
        this.keyStatus = KeyStatus.EXPIRED;
    }

    public void markCompromised() {
        this.keyStatus = KeyStatus.COMPROMISED;
    }

    // Utility Methods
    public String getMaskedKeyValue() {
        if (keyValue == null || keyValue.length() < 8) {
            return "****";
        }
        return keyValue.substring(0, 4) + "****" + keyValue.substring(keyValue.length() - 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerminalKey that = (TerminalKey) o;
        return Objects.equals(keyId, that.keyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyId);
    }

    @Override
    public String toString() {
        return "TerminalKey{" +
                "keyId=" + keyId +
                ", keyType='" + keyType + '\'' +
                ", keyStatus=" + keyStatus +
                ", keyLength=" + keyLength +
                ", maskedKey='" + getMaskedKeyValue() + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
