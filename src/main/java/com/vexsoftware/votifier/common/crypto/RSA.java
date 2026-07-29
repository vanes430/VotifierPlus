/*
 * Copyright (C) 2012 Vex Software LLC
 * Based on VotifierPlus by BenCodez (https://github.com/BenCodez/VotifierPlus).
 * Optimizations by vanes430.
 * This file is part of VotifierPlus.
 *
 * VotifierPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VotifierPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VotifierPlus.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.vexsoftware.votifier.common.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

/**
 * Static RSA utility methods for encrypting and decrypting blocks of
 * information.
 *
 * @author Blake Beaupain
 */
public class RSA {

	/**
	 * Encrypts a block of data.
	 *
	 * @param data
	 *            The data to encrypt
	 * @param key
	 *            The key to encrypt with
	 * @return The encrypted data
	 * @throws Exception
	 *             If an error occurs
	 */
	public static byte[] encrypt(byte[] data, PublicKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	/**
	 * Decrypts a block of data.
	 *
	 * @param data
	 *            The data to decrypt
	 * @param key
	 *            The key to decrypt with
	 * @return The decrypted data
	 * @throws Exception
	 *             If an error occurs
	 */
	public static byte[] decrypt(byte[] data, PrivateKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

}
