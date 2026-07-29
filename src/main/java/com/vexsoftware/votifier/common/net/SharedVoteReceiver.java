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
package com.vexsoftware.votifier.common.net;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;
import java.util.Set;

import com.vexsoftware.votifier.common.ForwardServer;
import com.vexsoftware.votifier.model.Vote;

public class SharedVoteReceiver extends VoteReceiver {

	private final VotePlatform platform;

	public SharedVoteReceiver(VotePlatform platform) throws Exception {
		super(platform.getHost(), platform.getPort());
		this.platform = platform;
	}

	@Override public void logWarning(String warn) {
		if (platform != null) { platform.logWarning(warn); return; }
		System.err.println("[VotifierPlus] WARN: " + warn);
	}
	@Override public void logSevere(String msg) {
		if (platform != null) { platform.logSevere(msg); return; }
		System.err.println("[VotifierPlus] SEVERE: " + msg);
	}
	@Override public void log(String msg) {
		if (platform != null) { platform.log(msg); return; }
		System.out.println("[VotifierPlus] INFO: " + msg);
	}
	@Override public void debug(String msg) {
		if (platform != null && platform.isDebug()) platform.debugMessage(msg);
	}
	@Override public void debug(Exception e) {
		if (platform != null && platform.isDebug()) platform.debugException(e);
	}
	@Override public String getVersion() { return platform.getVersion(); }
	@Override public Set<String> getServers() { return platform.getServers(); }
	@Override public ForwardServer getServerData(String s) { return platform.getServerData(s); }
	@Override public KeyPair getKeyPair() { return platform.getKeyPair(); }
	@Override public void callEvent(Vote vote) { platform.callEvent(vote); }
	@Override public Map<String, Key> getTokens() { return platform.getTokens(); }
	@Override public boolean isUseTokens() { return platform.isUseTokens(); }
	@Override public ThrottleConfig getThrottleConfig() { return platform.getThrottleConfig(); }
}
