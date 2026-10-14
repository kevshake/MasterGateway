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

	/** Returns true when every byte of a 32-character hex key has odd parity. */
	static boolean hasOddParityOnAllKeyBytes(String hexKey) {
		if (hexKey == null || hexKey.length() != 32) {
			return false;
		}
		for (int i = 0; i < 16; i++) {
			int high = Character.digit(hexKey.charAt(i * 2), 16);
			int low = Character.digit(hexKey.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				return false;
			}
			byte keyByte = (byte) ((high << 4) | low);
			if (Integer.bitCount(keyByte & 0xFF) % 2 == 0) {
				return false;
			}
		}
		return true;
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
