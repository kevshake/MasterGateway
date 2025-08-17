package com.kevshake.gateway.repository;

import com.kevshake.gateway.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Terminal entity operations
 * Provides CRUD operations and custom queries for terminal management
 */
@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    /**
     * Find terminal by Terminal ID (TID from ISO8583 field 41)
     * @param terminalId the terminal ID to search for
     * @return Optional containing the terminal if found
     */
    Optional<Terminal> findByTerminalId(String terminalId);

    /**
     * Check if a terminal exists by Terminal ID
     * @param terminalId the terminal ID to check
     * @return true if terminal exists, false otherwise
     */
    boolean existsByTerminalId(String terminalId);

    /**
     * Find all terminals by merchant ID (ISO8583 field 42)
     * @param merchantId the merchant ID to search for
     * @return List of terminals for the merchant
     */
    List<Terminal> findByMerchantId(String merchantId);

    /**
     * Find all terminals by status
     * @param status the terminal status to filter by
     * @return List of terminals with the specified status
     */
    List<Terminal> findByStatus(Terminal.TerminalStatus status);

    /**
     * Find all active terminals
     * @return List of active terminals
     */
    @Query("SELECT t FROM Terminal t WHERE t.status = 'ACTIVE'")
    List<Terminal> findAllActiveTerminals();

    /**
     * Find terminals that can process transactions (active with valid keys)
     * @return List of terminals ready for transactions
     */
    @Query("SELECT t FROM Terminal t WHERE t.status = 'ACTIVE' AND t.terminalKey IS NOT NULL AND t.terminalKey.keyStatus = 'ACTIVE'")
    List<Terminal> findTransactionReadyTerminals();

    /**
     * Find terminals by merchant ID and status
     * @param merchantId the merchant ID
     * @param status the terminal status
     * @return List of terminals matching criteria
     */
    List<Terminal> findByMerchantIdAndStatus(String merchantId, Terminal.TerminalStatus status);

    /**
     * Find terminals without assigned keys
     * @return List of terminals that need key assignment
     */
    @Query("SELECT t FROM Terminal t WHERE t.terminalKey IS NULL")
    List<Terminal> findTerminalsWithoutKeys();

    /**
     * Find terminals with expired keys
     * @return List of terminals with expired keys
     */
    @Query("SELECT t FROM Terminal t WHERE t.terminalKey IS NOT NULL AND " +
           "(t.terminalKey.keyStatus = 'EXPIRED' OR " +
           "(t.terminalKey.expiryDate IS NOT NULL AND t.terminalKey.expiryDate < CURRENT_TIMESTAMP))")
    List<Terminal> findTerminalsWithExpiredKeys();

    /**
     * Find terminals that haven't been active since a specific date
     * @param since the date to check activity since
     * @return List of inactive terminals
     */
    @Query("SELECT t FROM Terminal t WHERE t.lastActivityDate IS NULL OR t.lastActivityDate < :since")
    List<Terminal> findInactiveTerminalsSince(@Param("since") LocalDateTime since);

    /**
     * Find terminals by location (partial match)
     * @param location the location to search for (partial match)
     * @return List of terminals at the location
     */
    @Query("SELECT t FROM Terminal t WHERE LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Terminal> findByLocationContaining(@Param("location") String location);

    /**
     * Find terminals by terminal type
     * @param terminalType the type of terminal (POS, ATM, etc.)
     * @return List of terminals of the specified type
     */
    List<Terminal> findByTerminalType(String terminalType);

    /**
     * Get terminal count by status
     * @param status the status to count
     * @return count of terminals with the specified status
     */
    long countByStatus(Terminal.TerminalStatus status);

    /**
     * Get total count of active terminals
     * @return count of active terminals
     */
    @Query("SELECT COUNT(t) FROM Terminal t WHERE t.status = 'ACTIVE'")
    long countActiveTerminals();

    /**
     * Find terminals that need key changes (based on last change date)
     * @param cutoffDate date before which terminals need key changes
     * @return List of terminals requiring key changes
     */
    @Query("SELECT t FROM Terminal t WHERE " +
           "(t.lastKeyChangeDate IS NOT NULL AND t.lastKeyChangeDate < :cutoffDate) OR " +
           "(t.lastKeyChangeDate IS NULL AND t.createdDate < :cutoffDate)")
    List<Terminal> findTerminalsNeedingKeyChange(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find terminals created between dates
     * @param startDate start date range
     * @param endDate end date range
     * @return List of terminals created in the date range
     */
    @Query("SELECT t FROM Terminal t WHERE t.createdDate BETWEEN :startDate AND :endDate")
    List<Terminal> findTerminalsCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Update last activity date for a terminal
     * @param terminalId the terminal ID to update
     * @param activityDate the new activity date
     * @return number of records updated
     */
    @Query("UPDATE Terminal t SET t.lastActivityDate = :activityDate WHERE t.terminalId = :terminalId")
    int updateLastActivityDate(@Param("terminalId") String terminalId, @Param("activityDate") LocalDateTime activityDate);

    /**
     * Find terminals with specific serial number
     * @param serialNumber the serial number to search for
     * @return Optional containing the terminal if found
     */
    Optional<Terminal> findBySerialNumber(String serialNumber);

    /**
     * Custom query to find terminals with complex criteria
     * @param merchantId merchant ID (optional)
     * @param status terminal status (optional)
     * @param terminalType terminal type (optional)
     * @param hasKey whether terminal should have a key or not (optional)
     * @return List of terminals matching the criteria
     */
    @Query("SELECT t FROM Terminal t WHERE " +
           "(:merchantId IS NULL OR t.merchantId = :merchantId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:terminalType IS NULL OR t.terminalType = :terminalType) AND " +
           "(:hasKey IS NULL OR " +
           " (:hasKey = true AND t.terminalKey IS NOT NULL) OR " +
           " (:hasKey = false AND t.terminalKey IS NULL))")
    List<Terminal> findTerminalsByCriteria(
        @Param("merchantId") String merchantId,
        @Param("status") Terminal.TerminalStatus status,
        @Param("terminalType") String terminalType,
        @Param("hasKey") Boolean hasKey);
}
