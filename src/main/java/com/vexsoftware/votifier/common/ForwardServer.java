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
package com.vexsoftware.votifier.common;

import java.security.Key;

public class ForwardServer {

	private String host;
	private int port;
	private String key;
	private boolean enabled;
	private Key Token;

	public ForwardServer(boolean enabled, String host, int port, String key, Key token) {
		this.enabled = enabled;
		this.host = host;
		this.port = port;
		this.key = key;
		this.Token = token;
	}

	public String getHost() { return host; }
	public int getPort() { return port; }
	public String getKey() { return key; }
	public boolean isEnabled() { return enabled; }
	public Key getToken() { return Token; }
	public void setToken(Key token) { this.Token = token; }
	public boolean isUseTokens() { return Token != null; }
}
