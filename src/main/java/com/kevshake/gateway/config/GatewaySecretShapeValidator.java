package com.kevshake.gateway.config;

import java.util.Locale;

/**
 * Validates persisted gateway secret shapes without logging secret values.
 */
final class GatewaySecretShapeValidator {

	private static final int MIN_PASSWORD_LENGTH = 16;

	private static final String TDES_HEX_PATTERN = "[0-9A-F]{32}";

	private GatewaySecretShapeValidator() {
	}

	/** Validates a password entry loaded from the generated defaults file. */
	static void validatePasswordEntry(String entryKey, String value) {
		if (value == null || value.length() < MIN_PASSWORD_LENGTH) {
			throw invalidEntry(entryKey, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
		}
	}

	/** Validates a TDES hex key entry loaded from the generated defaults file. */
	static void validateTdesKeyEntry(String entryKey, String value) {
		if (value == null) {
			throw invalidEntry(entryKey, "TDES key is missing.");
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (!normalized.matches(TDES_HEX_PATTERN)) {
			throw invalidEntry(entryKey, "TDES key must be 32 uppercase hex characters.");
		}
		if (!GatewayTdesKeyMaterialGenerator.hasOddParityOnAllKeyBytes(normalized)) {
			throw invalidEntry(entryKey, "TDES key bytes must have odd parity.");
		}
	}

	/** Validates the loopback-or-host entry for bank host defaults. */
	static void validateBankHostEntry(String entryKey, String value) {
		if (value == null || value.isBlank()) {
			throw invalidEntry(entryKey, "Bank host must not be blank.");
		}
	}

	private static IllegalStateException invalidEntry(String entryKey, String problem) {
		return new IllegalStateException("[" + GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV + "] Invalid entry key "
				+ entryKey + ": " + problem);
	}
}
