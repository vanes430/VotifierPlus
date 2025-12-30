package com.vexsoftware.votifier.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

public class TokenUtil {
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String newToken() {
		// Generate 8 bytes (64 bits) of cryptographically secure random data
		byte[] bytes = new byte[8];
		RANDOM.nextBytes(bytes);
		
		// Convert to 16-character lowercase hexadecimal string
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
	
	 public static Key createKeyFrom(String token) {
	        return new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	    }
}
