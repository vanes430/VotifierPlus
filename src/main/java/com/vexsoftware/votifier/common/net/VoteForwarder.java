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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.common.ForwardServer;
import com.vexsoftware.votifier.model.Vote;

public class VoteForwarder {

	private static final String HANDSHAKE_PREFIX = "VOTIFIER";
	private static final String HANDSHAKE_V2 = "2";
	private static final String OPCODE_VOTE = "VOTE";
	private static final String FIELD_PAYLOAD = "payload";
	private static final String FIELD_SIGNATURE = "signature";
	private static final String FIELD_SERVICE_NAME = "serviceName";
	private static final String FIELD_USERNAME = "username";
	private static final String FIELD_ADDRESS = "address";
	private static final String FIELD_TIMESTAMP = "timestamp";
	private static final String FIELD_CHALLENGE = "challenge";

	private final VoteReceiver receiver;

	public VoteForwarder(VoteReceiver receiver) {
		this.receiver = receiver;
	}

	public void forwardVote(Vote vote) {
		for (String name : receiver.getServers()) {
			ForwardServer server = receiver.getServerData(name);

			if (!server.isEnabled()) {
				receiver.debug("Skipping disabled forward server: " + name);
				continue;
			}

			receiver.debug("Preparing to forward vote to: " + name + ", tokens mode: " + server.isUseTokens());

			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(server.getHost(), server.getPort()), 1000);
				socket.setSoTimeout(3000);

				BufferedReader in = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				OutputStream out = socket.getOutputStream();

				String greeting = in.readLine();
				receiver.debug("Received handshake from " + name + ": '" + greeting + "'");

				byte[] payload;
				if (server.isUseTokens()) {
					String[] parts = greeting.split(" ");
					if (parts.length < 3 || !HANDSHAKE_PREFIX.equals(parts[0]) || !HANDSHAKE_V2.equals(parts[1])) {
						throw new IllegalStateException("Invalid token-mode handshake from " + name + ": " + greeting);
					}

					String challenge = parts[2];

					JsonObject inner = new JsonObject();
					inner.addProperty(FIELD_SERVICE_NAME, vote.getServiceName());
					inner.addProperty(FIELD_USERNAME, vote.getUsername());
					inner.addProperty(FIELD_ADDRESS, vote.getAddress());
					inner.addProperty(FIELD_TIMESTAMP, vote.getTimeStamp());
					inner.addProperty(FIELD_CHALLENGE, challenge);

					String innerJson = inner.toString();

					Mac mac = Mac.getInstance("HmacSHA256");
					mac.init(server.getToken());
					String sig = Base64.getEncoder()
							.encodeToString(mac.doFinal(innerJson.getBytes(StandardCharsets.UTF_8)));

					JsonObject outer = new JsonObject();
					outer.addProperty(FIELD_PAYLOAD, innerJson);
					outer.addProperty(FIELD_SIGNATURE, sig);

					payload = (outer.toString() + "\r\n").getBytes(StandardCharsets.UTF_8);
				} else {
					String voteString = String.join("\n", OPCODE_VOTE, vote.getServiceName(), vote.getUsername(),
							vote.getAddress(), vote.getTimeStamp(), "") + "\n";
					payload = receiver.encrypt(voteString.getBytes(StandardCharsets.UTF_8), receiver.getPublicKey(server));
				}

				out.write(payload);
				out.flush();
				receiver.debug("Payload forwarded to " + name + " (" + payload.length + " bytes)");
			} catch (Exception ex) {
				receiver.log("Failed to forward vote to " + name + ": " + ex.getClass().getSimpleName() + " - "
						+ ex.getMessage());
			}
		}
	}
}
