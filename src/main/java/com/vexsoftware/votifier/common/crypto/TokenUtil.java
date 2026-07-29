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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

public class TokenUtil {
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String newToken() {
		return new BigInteger(130, RANDOM).toString(32);
	}

	 public static Key createKeyFrom(String token) {
	        return new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	    }
}
