package com.kevshake.gateway.config;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Generates double-length TDES key hex with odd parity per DES key byte.
 */
final class GatewayTdesKeyMaterialGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();

	private GatewayTdesKeyMaterialGenerator() {
	}

	/** Produces 32 hex characters (16 bytes) suitable for double-length TDES. */
	static String generateDoubleLengthTdesHexKey() {
		byte[] keyBytes = new byte[16];
		RANDOM.nextBytes(keyBytes);
		for (int i = 0; i < keyBytes.length; i++) {
			keyBytes[i] = applyOddParity(keyBytes[i]);
		}
		return HexFormat.of().formatHex(keyBytes).toUpperCase(Locale.ROOT);
	}

	/** Sets the low bit so the byte has odd parity (DES key byte convention). */
	private static byte applyOddParity(byte value) {
		int unsigned = value & 0xFF;
		unsigned &= 0xFE;
		int ones = Integer.bitCount(unsigned);
		if (ones % 2 == 0) {
			unsigned |= 0x01;
		}
		return (byte) unsigned;
	}
}
