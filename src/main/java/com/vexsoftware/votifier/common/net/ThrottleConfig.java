/*
 * Copyright (C) 2012 Vex Software LLC
 * Optimizations by vanes430.
 * This file is part of Votifier.
 *
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.vexsoftware.votifier.common.net;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ThrottleConfig {

	public final boolean enabled;
	public final Set<String> tunnelRemoteIps;
	public final long windowMs;
	public final int failures;
	public final long throttleForMs;
	public final int tunnelFailures;
	public final long tunnelThrottleForMs;
	public final boolean perClientBanEnabled;
	public final int perClientBanFailures;
	public final long perClientBanForMs;
	public final long logWindowMs;

	public ThrottleConfig(boolean enabled, Set<String> tunnelRemoteIps, String window, int failures,
			String throttleFor, int tunnelFailures, String tunnelThrottleFor, boolean perClientBanEnabled,
			int perClientBanFailures, String perClientBanFor, String logWindow) {
		this.enabled = enabled;

		if (tunnelRemoteIps == null || tunnelRemoteIps.isEmpty()) {
			this.tunnelRemoteIps = Collections.emptySet();
		} else {
			this.tunnelRemoteIps = Collections.unmodifiableSet(new HashSet<String>(tunnelRemoteIps));
		}

		this.windowMs = safeDurationMs(window, 2 * 60_000L);
		this.failures = failures;
		this.throttleForMs = safeDurationMs(throttleFor, 5 * 60_000L);
		this.tunnelFailures = tunnelFailures;
		this.tunnelThrottleForMs = safeDurationMs(tunnelThrottleFor, 10 * 60_000L);
		this.perClientBanEnabled = perClientBanEnabled;
		this.perClientBanFailures = perClientBanFailures;
		this.perClientBanForMs = safeDurationMs(perClientBanFor, 15 * 60_000L);
		this.logWindowMs = safeDurationMs(logWindow, 60_000L);
	}

	private static long safeDurationMs(String raw, long fallback) {
		try {
			if (raw == null || raw.isEmpty()) {
				return fallback;
			}
			return parseDuration(raw, fallback);
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static long parseDuration(String raw, long fallbackMs) {
		String s = raw.trim().toLowerCase();
		if (s.isEmpty()) return fallbackMs;

		int numStart = 0;
		while (numStart < s.length() && Character.isDigit(s.charAt(numStart))) {
			numStart++;
		}
		if (numStart == 0) return fallbackMs;

		long amount;
		try {
			amount = Long.parseLong(s.substring(0, numStart));
		} catch (NumberFormatException e) {
			return fallbackMs;
		}
		if (amount <= 0) return fallbackMs;

		String unit = s.substring(numStart).trim();
		switch (unit) {
			case "ms": return amount;
			case "s":  return amount * 1000L;
			case "m":  return amount * 60_000L;
			case "h":  return amount * 3_600_000L;
			case "d":  return amount * 86_400_000L;
			default:   return fallbackMs;
		}
	}
}
