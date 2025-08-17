package com.kevshake.gateway.repository;

import com.kevshake.gateway.entity.TerminalKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TerminalKey entity operations
 * Provides CRUD operations and custom queries for key management
 */
@Repository
public interface TerminalKeyRepository extends JpaRepository<TerminalKey, Long> {

    /**
     * Find key by key value (for duplicate checking)
     * @param keyValue the key value to search for
     * @return Optional containing the key if found
     */
    Optional<TerminalKey> findByKeyValue(String keyValue);

    /**
     * Check if a key value already exists
     * @param keyValue the key value to check
     * @return true if key exists, false otherwise
     */
    boolean existsByKeyValue(String keyValue);

    /**
     * Find all keys by status
     * @param keyStatus the key status to filter by
     * @return List of keys with the specified status
     */
    List<TerminalKey> findByKeyStatus(TerminalKey.KeyStatus keyStatus);

    /**
     * Find all active keys
     * @return List of active keys
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.keyStatus = 'ACTIVE'")
    List<TerminalKey> findAllActiveKeys();

    /**
     * Find all keys by type
     * @param keyType the key type to filter by (TDES, AES, etc.)
     * @return List of keys of the specified type
     */
    List<TerminalKey> findByKeyType(String keyType);

    /**
     * Find keys by key length
     * @param keyLength the key length (2 for double-length, 3 for triple-length)
     * @return List of keys with the specified length
     */
    List<TerminalKey> findByKeyLength(Integer keyLength);

    /**
     * Find expired keys
     * @return List of expired keys (either status EXPIRED or past expiry date)
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.keyStatus = 'EXPIRED' OR " +
           "(tk.expiryDate IS NOT NULL AND tk.expiryDate < CURRENT_TIMESTAMP)")
    List<TerminalKey> findExpiredKeys();

    /**
     * Find keys expiring within a specified number of days
     * @param days number of days to check ahead
     * @return List of keys expiring soon
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.expiryDate IS NOT NULL AND " +
           "tk.expiryDate BETWEEN CURRENT_TIMESTAMP AND :expiryDate")
    List<TerminalKey> findKeysExpiringWithinDays(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Find keys created between dates
     * @param startDate start date range
     * @param endDate end date range
     * @return List of keys created in the date range
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.createdDate BETWEEN :startDate AND :endDate")
    List<TerminalKey> findKeysCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Find orphaned keys (keys not associated with any terminal)
     * @return List of orphaned keys
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.terminal IS NULL")
    List<TerminalKey> findOrphanedKeys();

    /**
     * Find keys by Key Check Value (KCV)
     * @param keyCheckValue the KCV to search for
     * @return Optional containing the key if found
     */
    Optional<TerminalKey> findByKeyCheckValue(String keyCheckValue);

    /**
     * Count keys by status
     * @param keyStatus the status to count
     * @return count of keys with the specified status
     */
    long countByKeyStatus(TerminalKey.KeyStatus keyStatus);

    /**
     * Count active keys
     * @return count of active keys
     */
    @Query("SELECT COUNT(tk) FROM TerminalKey tk WHERE tk.keyStatus = 'ACTIVE'")
    long countActiveKeys();

    /**
     * Find keys by type and status
     * @param keyType the key type
     * @param keyStatus the key status
     * @return List of keys matching both criteria
     */
    List<TerminalKey> findByKeyTypeAndKeyStatus(String keyType, TerminalKey.KeyStatus keyStatus);

    /**
     * Find keys by length and status
     * @param keyLength the key length
     * @param keyStatus the key status
     * @return List of keys matching both criteria
     */
    List<TerminalKey> findByKeyLengthAndKeyStatus(Integer keyLength, TerminalKey.KeyStatus keyStatus);

    /**
     * Find the most recently created key for a specific terminal
     * @param terminalId the terminal ID to search for
     * @return Optional containing the most recent key
     */
    @Query("SELECT tk FROM TerminalKey tk JOIN tk.terminal t WHERE t.terminalId = :terminalId " +
           "ORDER BY tk.createdDate DESC")
    List<TerminalKey> findKeysByTerminalIdOrderByCreatedDateDesc(@Param("terminalId") String terminalId);

    /**
     * Find keys that need to be rotated (old keys that should be replaced)
     * @param maxAgeInDays maximum age of keys in days before rotation needed
     * @return List of keys that need rotation
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.keyStatus = 'ACTIVE' AND " +
           "tk.createdDate < :cutoffDate")
    List<TerminalKey> findKeysNeedingRotation(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find compromised keys
     * @return List of compromised keys
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.keyStatus = 'COMPROMISED'")
    List<TerminalKey> findCompromisedKeys();

    /**
     * Find pending keys (keys awaiting activation)
     * @return List of pending keys
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE tk.keyStatus = 'PENDING'")
    List<TerminalKey> findPendingKeys();

    /**
     * Update key status
     * @param keyId the key ID to update
     * @param newStatus the new status
     * @return number of records updated
     */
    @Query("UPDATE TerminalKey tk SET tk.keyStatus = :newStatus, tk.updatedDate = CURRENT_TIMESTAMP " +
           "WHERE tk.keyId = :keyId")
    int updateKeyStatus(@Param("keyId") Long keyId, @Param("newStatus") TerminalKey.KeyStatus newStatus);

    /**
     * Bulk update status for multiple keys
     * @param keyIds list of key IDs to update
     * @param newStatus the new status
     * @return number of records updated
     */
    @Query("UPDATE TerminalKey tk SET tk.keyStatus = :newStatus, tk.updatedDate = CURRENT_TIMESTAMP " +
           "WHERE tk.keyId IN :keyIds")
    int updateMultipleKeyStatuses(@Param("keyIds") List<Long> keyIds, @Param("newStatus") TerminalKey.KeyStatus newStatus);

    /**
     * Find keys with specific notes (partial match)
     * @param notes the notes to search for (partial match)
     * @return List of keys with matching notes
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE LOWER(tk.notes) LIKE LOWER(CONCAT('%', :notes, '%'))")
    List<TerminalKey> findByNotesContaining(@Param("notes") String notes);

    /**
     * Get key statistics by type and status
     * @return List of key statistics
     */
    @Query("SELECT tk.keyType, tk.keyStatus, COUNT(tk) FROM TerminalKey tk " +
           "GROUP BY tk.keyType, tk.keyStatus ORDER BY tk.keyType, tk.keyStatus")
    List<Object[]> getKeyStatistics();

    /**
     * Find duplicate keys (same key value but different IDs)
     * @return List of duplicate key values
     */
    @Query("SELECT tk.keyValue FROM TerminalKey tk GROUP BY tk.keyValue HAVING COUNT(tk) > 1")
    List<String> findDuplicateKeyValues();

    /**
     * Custom query to find keys with complex criteria
     * @param keyType key type (optional)
     * @param keyStatus key status (optional)
     * @param keyLength key length (optional)
     * @param hasTerminal whether key should be assigned to a terminal (optional)
     * @return List of keys matching the criteria
     */
    @Query("SELECT tk FROM TerminalKey tk WHERE " +
           "(:keyType IS NULL OR tk.keyType = :keyType) AND " +
           "(:keyStatus IS NULL OR tk.keyStatus = :keyStatus) AND " +
           "(:keyLength IS NULL OR tk.keyLength = :keyLength) AND " +
           "(:hasTerminal IS NULL OR " +
           " (:hasTerminal = true AND tk.terminal IS NOT NULL) OR " +
           " (:hasTerminal = false AND tk.terminal IS NULL))")
    List<TerminalKey> findKeysByCriteria(
        @Param("keyType") String keyType,
        @Param("keyStatus") TerminalKey.KeyStatus keyStatus,
        @Param("keyLength") Integer keyLength,
        @Param("hasTerminal") Boolean hasTerminal);
}
