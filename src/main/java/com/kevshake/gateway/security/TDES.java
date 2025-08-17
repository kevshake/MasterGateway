package com.kevshake.gateway.security;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

//Java code for the above approach
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.codec.binary.Hex;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * TDES (Triple DES) Security Class for PIN encryption/decryption
 * Used for internal PIN processing without physical HSM
 */
public class TDES {
	private static class DES {
		// CONSTANTS
		// Initial Permutation Table
		int[] IP
		= { 58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44,
				36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22,
				14, 6, 64, 56, 48, 40, 32, 24, 16, 8, 57,
				49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35,
				27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13,
				5, 63, 55, 47, 39, 31, 23, 15, 7 };

		// Inverse Initial Permutation Table
		int[] IP1
		= { 40, 8, 48, 16, 56, 24, 64, 32, 39, 7, 47,
				15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22,
				62, 30, 37, 5, 45, 13, 53, 21, 61, 29, 36,
				4, 44, 12, 52, 20, 60, 28, 35, 3, 43, 11,
				51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58,
				26, 33, 1, 41, 9, 49, 17, 57, 25 };

		// first key-hePermutation Table
		int[] PC1
		= { 57, 49, 41, 33, 25, 17, 9, 1, 58, 50,
				42, 34, 26, 18, 10, 2, 59, 51, 43, 35,
				27, 19, 11, 3, 60, 52, 44, 36, 63, 55,
				47, 39, 31, 23, 15, 7, 62, 54, 46, 38,
				30, 22, 14, 6, 61, 53, 45, 37, 29, 21,
				13, 5, 28, 20, 12, 4 };

		// second key-Permutation Table
		int[] PC2
		= { 14, 17, 11, 24, 1, 5, 3, 28, 15, 6,
				21, 10, 23, 19, 12, 4, 26, 8, 16, 7,
				27, 20, 13, 2, 41, 52, 31, 37, 47, 55,
				30, 40, 51, 45, 33, 48, 44, 49, 39, 56,
				34, 53, 46, 42, 50, 36, 29, 32 };

		// Expansion D-box Table
		int[] EP = { 32, 1, 2, 3, 4, 5, 4, 5, 6, 7,
				8, 9, 8, 9, 10, 11, 12, 13, 12, 13,
				14, 15, 16, 17, 16, 17, 18, 19, 20, 21,
				20, 21, 22, 23, 24, 25, 24, 25, 26, 27,
				28, 29, 28, 29, 30, 31, 32, 1 };

		// Straight Permutation Table
		int[] P
		= { 16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23,
				26, 5, 18, 31, 10, 2, 8, 24, 14, 32, 27,
				3, 9, 19, 13, 30, 6, 22, 11, 4, 25 };

		// S-box Table
		int[][][] sbox
		= { { { 14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6,
			12, 5, 9, 0, 7 },
			{ 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12,
				11, 9, 5, 3, 8 },
			{ 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7,
					3, 10, 5, 0 },
			{ 15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14,
						10, 0, 6, 13 } },

				{ { 15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13,
					12, 0, 5, 10 },
							{ 3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10,
						6, 9, 11, 5 },
							{ 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6,
							9, 3, 2, 15 },
							{ 13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12,
								0, 5, 14, 9 } },
				{ { 10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7,
					11, 4, 2, 8 },
									{ 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14,
						12, 11, 15, 1 },
									{ 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12,
							5, 10, 14, 7 },
									{ 1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3,
								11, 5, 2, 12 } },
				{ { 7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5,
					11, 12, 4, 15 },
									{ 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12,
						1, 10, 14, 9 },
									{ 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3,
							14, 5, 2, 8, 4 },
									{ 3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11,
								12, 7, 2, 14 } },
				{ { 2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15,
					13, 0, 14, 9 },
									{ 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15,
						10, 3, 9, 8, 6 },
									{ 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5,
							6, 3, 0, 14 },
									{ 11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9,
								10, 4, 5, 3 } },
				{ { 12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4,
					14, 7, 5, 11 },
									{ 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14,
						0, 11, 3, 8 },
									{ 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10,
							1, 13, 11, 6 },
									{ 4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7,
								6, 0, 8, 13 } },
				{ { 4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7,
					5, 10, 6, 1 },
									{ 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12,
						2, 15, 8, 6 },
									{ 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6,
							8, 0, 5, 9, 2 },
									{ 6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15,
								14, 2, 3, 12 } },
				{ { 13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14,
					5, 0, 12, 7 },
									{ 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11,
						0, 14, 9, 2 },
									{ 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13,
							15, 3, 5, 8 },
									{ 2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0,
								3, 5, 6, 11 } } };
		int[] shiftBits = { 1, 1, 2, 2, 2, 2, 2, 2,
				1, 2, 2, 2, 2, 2, 2, 1 };

		// hexadecimal to binary conversion
		String hextoBin(String input) {
			int n = input.length() * 4;
			input = Long.toBinaryString(
					Long.parseUnsignedLong(input, 16));
			while (input.length() < n)
				input = "0" + input;
			return input;
		}

		// binary to hexadecimal conversion
		String binToHex(String input) {
			int n = (int)input.length() / 4;
			input = Long.toHexString(
					Long.parseUnsignedLong(input, 2));
			while (input.length() < n)
				input = "0" + input;
			return input;
		}

		// per-mutate input hexadecimal
		// according to specified sequence
		String permutation(int[] sequence, String input) {
			String output = "";
			input = hextoBin(input);
			for (int i = 0; i < sequence.length; i++)
				output += input.charAt(sequence[i] - 1);
			output = binToHex(output);
			return output;
		}

		// xor 2 hexadecimal strings
		String xor(String a, String b) {
			// hexadecimal to decimal(base 10)
			long t_a = Long.parseUnsignedLong(a, 16);
			// hexadecimal to decimal(base 10)
			long t_b = Long.parseUnsignedLong(b, 16);
			// xor
			t_a = t_a ^ t_b;
			// decimal to hexadecimal
			a = Long.toHexString(t_a);
			// prepend 0's to maintain length
			while (a.length() < b.length())
				a = "0" + a;
			return a;
		}

		// left Circular Shifting bits
		String leftCircularShift(String input, int numBits)
		{
			int n = input.length() * 4;
			int perm[] = new int[n];
			for (int i = 0; i < n - 1; i++)
				perm[i] = (i + 2);
			perm[n - 1] = 1;
			while (numBits-- > 0)
				input = permutation(perm, input);
			return input;
		}

		// preparing 16 keys for 16 rounds
		String[] getKeys(String key)
		{
			String keys[] = new String[16];
			// first key permutation
			key = permutation(PC1, key);
			for (int i = 0; i < 16; i++) {
				key = leftCircularShift(key.substring(0, 7),
						shiftBits[i])
						+ leftCircularShift(
								key.substring(7, 14),
								shiftBits[i]);
				// second key permutation
				keys[i] = permutation(PC2, key);
			}
			return keys;
		}

		// s-box lookup
		String sBox(String input)
		{
			String output = "";
			input = hextoBin(input);
			for (int i = 0; i < 48; i += 6) {
				String temp = input.substring(i, i + 6);
				int num = i / 6;
				int row = Integer.parseInt(
						temp.charAt(0) + "" + temp.charAt(5),
						2);
				int col = Integer.parseInt(
						temp.substring(1, 5), 2);
				output += Integer.toHexString(
						sbox[num][row][col]);
			}
			return output;
		}

		String round(String input, String key, int num) {
			// fk
			String left = input.substring(0, 8);
			String temp = input.substring(8, 16);
			String right = temp;
			// Expansion permutation
			temp = permutation(EP, temp);
			// xor temp and round key
			temp = xor(temp, key);
			// lookup in s-box table
			temp = sBox(temp);
			// Straight D-box
			temp = permutation(P, temp);
			// xor
			left = xor(left, temp);

			// swapper
			return right + left;
		}

		String encrypt(String plainText, String key) {
			int i;
			// get round keys
			String keys[] = getKeys(key);

			// initial permutation
			plainText = permutation(IP, plainText);

			// 16 rounds
			for (i = 0; i < 16; i++) {
				plainText = round(plainText, keys[i], i);
			}

			// 32-bit swap
			plainText = plainText.substring(8, 16)
					+ plainText.substring(0, 8);

			// final permutation
			plainText = permutation(IP1, plainText);
			return plainText;
		}

		String decrypt(String plainText, String key) {
			int i;
			// get round keys
			String keys[] = getKeys(key);

			// initial permutation
			plainText = permutation(IP, plainText);

			// 16-rounds
			for (i = 15; i > -1; i--) {
				plainText
				= round(plainText, keys[i], 15 - i);
			}

			// 32-bit swap
			plainText = plainText.substring(8, 16)
					+ plainText.substring(0, 8);
			plainText = permutation(IP1, plainText);
			return plainText;
		}
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Generate TDES key for encryption/decryption
	 * @param KeyLength 1 for single DES, 2 for double DES, 3 for triple DES
	 * @return Generated TDES key in hexadecimal format
	 */
	public static String keygenerator(int KeyLength) {
		KeyGenerator keygenerator;
		String OutDesKey = "";

		try {
			keygenerator = KeyGenerator.getInstance("DES");
			SecretKey desKey = keygenerator.generateKey();

			OutDesKey = new String(bytesToHex(desKey.getEncoded())).toUpperCase();
			if(KeyLength>2) {
				SecretKey desKey2 = keygenerator.generateKey();
				OutDesKey = OutDesKey + new String(bytesToHex(desKey2.getEncoded())).toUpperCase();

				SecretKey desKey3 = keygenerator.generateKey();
				OutDesKey = OutDesKey + new String(bytesToHex(desKey3.getEncoded())).toUpperCase();
			}else if(KeyLength>1) {
				SecretKey desKey2 = keygenerator.generateKey();
				OutDesKey = OutDesKey + new String(bytesToHex(desKey2.getEncoded())).toUpperCase();
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("TDES New Key: " + OutDesKey);

		return OutDesKey.toUpperCase();
	}//end of fn:keygenerator

	/**
	 * TDES Encryption
	 * @param ClearText Clear text to encrypt (hexadecimal)
	 * @param Key TDES key (hexadecimal)
	 * @param abc true for 3DES (3 keys), false for 2DES (2 keys)
	 * @return Encrypted text in hexadecimal
	 */
	public static String TDES_Encrypt(String ClearText, String Key, boolean abc) {
		String key1 = "";
		String Key2 = "";
		String Key3 = "";

		String text = "";
		String text2 = "";
		String text3 = "";

		String data = "";
		String data2 = "";
		String data3 = "";
		String Output = "";

		DES cipher = new DES();

		try {
			key1 = Key.substring(0, 16).toUpperCase();
			data = ClearText.substring(0, 16).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			Key2 = Key.substring(16, 32).toUpperCase();
			data2 = ClearText.substring(16, 32).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			Key3 = Key.substring(32, 48).toUpperCase();
			data3 = ClearText.substring(32, 48).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			//L1
			text = cipher.encrypt(data.toUpperCase(), key1);
			text = cipher.decrypt(text, Key2);
			if (abc) { text = cipher.encrypt(text, Key3); } else { text = cipher.encrypt(text, key1); }

			Output += text;
			if (ClearText.length()>16){
				//L2
				text2 = cipher.encrypt(data2.toUpperCase(), key1);
				text2 = cipher.decrypt(text2, Key2);
				if (abc) { text2 = cipher.encrypt(text2, Key3); } else { text2 = cipher.encrypt(text2, key1); }	

				Output += text2;
			}

			if (ClearText.length()>32) {
				//L3
				text3 = cipher.encrypt(data3.toUpperCase(), key1);
				text3 = cipher.decrypt(text3, Key2);
				if (abc) { text3 = cipher.encrypt(text3, Key3); } else { text3 = cipher.encrypt(text3, key1); }

				Output += text3;
			} 
		} catch (Exception e) {
			// TODO: handle exception
		}

		return Output.toUpperCase();
	}

	/**
	 * Extract right-most 12 digits of the primary account number (PAN)
	 * @param accountNumber PAN - primary account number
	 * @return Account number part for PIN block calculation
	 */
	public static String extractPanAccountNumberPart(String accountNumber) {
		String accountNumberPart = null;
		if (accountNumber.length() > 12)
			accountNumberPart = accountNumber.substring(accountNumber.length() - 13, accountNumber.length() - 1);
		else
			accountNumberPart = accountNumber;
		return accountNumberPart;
	}

	/**
	 * Encode PIN to PIN block format 0 - ISO 9564
	 * @param pin Clear PIN
	 * @param pan Primary Account Number
	 * @return PIN block in format 0 (hexadecimal)
	 */
	public static String format0Encode(String pin, String pan) {
		try {
			final String pinLenHead = StringUtils.leftPad(Integer.toString(pin.length()), 2, '0')+pin;
			final String pinData = StringUtils.rightPad(pinLenHead, 16,'F');
			final byte[] bPin = Hex.decodeHex(pinData.toCharArray());
			final String panPart = extractPanAccountNumberPart(pan);
			final String panData = StringUtils.leftPad(panPart, 16, '0');
			final byte[] bPan = Hex.decodeHex(panData.toCharArray());

			final byte[] pinblock = new byte[8];
			for (int i = 0; i < 8; i++)
				pinblock[i] = (byte) (bPin[i] ^ bPan[i]);

			return String.valueOf(Hex.encodeHex(pinblock)).toUpperCase();
		} catch (DecoderException e) {
			throw new RuntimeException("Hex decoder failed!",e);
		}
	}

	/**
	 * Decode pinblock format 0 - ISO 9564
	 * @param pinblock pinblock in format 0 - ISO 9564 in HEX format 
	 * @param pan primary account number (PAN/CLN/CardNumber)
	 * @return clear PIN
	 */
	public static String format0decode(String pinblock, String pan) {
		try {
			final String panPart = extractPanAccountNumberPart(pan);
			final String panData = StringUtils.leftPad(panPart, 16, '0');
			final byte[] bPan = Hex.decodeHex(panData.toCharArray());

			final byte[] bPinBlock = Hex.decodeHex(pinblock.toCharArray());

			final byte[] bPin  = new byte[8];
			for (int i = 0; i < 8; i++)
				bPin[i] = (byte) (bPinBlock[i] ^ bPan[i]);

			final String pinData =  String.valueOf(Hex.encodeHex(bPin)).toUpperCase();
			final int pinLen = Integer.parseInt(pinData.substring(0, 2));
			return pinData.substring(2,2+pinLen);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Invalid pinblock format!");
		} catch (DecoderException e) {
			throw new RuntimeException("Hex decoder failed!",e);
		}
	}

	/**
	 * TDES Decryption
	 * @param CipheredText Encrypted text (hexadecimal)
	 * @param Key TDES key (hexadecimal)
	 * @param abc true for 3DES (3 keys), false for 2DES (2 keys)
	 * @return Decrypted text in hexadecimal
	 */
	public static String TDES_Decrypt(String CipheredText, String Key, boolean abc) {
		String key1 = "";
		String Key2 = "";
		String Key3 = "";

		String text = "";
		String text2 = "";
		String text3 = "";

		String data = "";
		String data2 = "";
		String data3 = "";
		String Output = "";

		DES cipher = new DES();

		try {
			key1 = Key.substring(0, 16).toUpperCase();
			data = CipheredText.substring(0, 16).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			Key2 = Key.substring(16, 32).toUpperCase();
			data2 = CipheredText.substring(16, 32).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			Key3 = Key.substring(32, 48).toUpperCase();
			data3 = CipheredText.substring(32, 48).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			//L1
			text = cipher.decrypt(data.toUpperCase(), key1);
			text = cipher.encrypt(text, Key2);
			if (abc) { text = cipher.decrypt(text, Key3); } else { text = cipher.decrypt(text, key1); }

			Output += text;
			if (CipheredText.length()>16){
				//L2
				text2 = cipher.decrypt(data2.toUpperCase(), key1);
				text2 = cipher.encrypt(text2, Key2);
				if (abc) { text2 = cipher.decrypt(text2, Key3); } else { text2 = cipher.decrypt(text2, key1); }	

				Output += text2;
			}

			if (CipheredText.length()>32) {
				//L3
				text3 = cipher.decrypt(data3.toUpperCase(), key1);
				text3 = cipher.encrypt(text3, Key2);
				if (abc) { text3 = cipher.decrypt(text3, Key3); } else { text3 = cipher.decrypt(text3, key1); }

				Output += text3;
			} 
		} catch (Exception e) {
			// TODO: handle exception
		}

		return Output.toUpperCase();
	}

	/**
	 * Get Key Check Value (KCV) for key verification
	 * @param KeyLoaded TDES key to get KCV for
	 * @return KCV (first 6 characters of encrypted zero block)
	 */
	public static String getKCV(String KeyLoaded) {
		String KCV = "";
		String IV = "0000000000000000";

		KCV = TDES_Encrypt(IV, KeyLoaded, false).toUpperCase().substring(0,6).toUpperCase();

		return KCV;
	}

	/**
	 * Test method for TDES functionality
	 */
	public static void test() {
		String text = "123456ABCD132536";
		String PIN = "041041CF9FFFFFDF";
		String key  = "AABB09182736CCDD";

		String Tdeskey = "9E4F7FF1F831F1132CD9B6C740B0134C";
		String TdesData = "041223C6FFEFEFFE";
		String CipheredTdesData = "";

		String MKEY = "40763BB5B0B910B5CE3297E58967CD2A";
		String   TKEY_Ecrypted = "C5B1132B579E0377107A7B9F2C112073";
		String PinRaw = "1234";
		String CardNumber = "4761739001010010";
		String PinBlockRaw = "";
		String TKEY = "";

		//TDES Keys testing
		System.out.println("TDES Encryption:\n");
		System.out.println( "\nEncrypted Key : " + TKEY_Ecrypted.toUpperCase() + "\n");
		TKEY = TDES_Decrypt(TKEY_Ecrypted, MKEY, false);
		System.out.println( "\nPlain Key: " + TKEY.toUpperCase() + "\n");
		Tdeskey = TKEY;

		//Get PINBLOCK raw
		PinBlockRaw = format0Encode(PinRaw, CardNumber);

		//TDES testing
		System.out.println("TDES Encryption:\n");
		System.out.println( "Card: "+CardNumber+" PIN data is "+PinBlockRaw+"\n");
		CipheredTdesData = TDES_Encrypt(PinBlockRaw, Tdeskey, false);
		System.out.println( "\nCipher Text: " + CipheredTdesData.toUpperCase() + "\n");
	}
}
