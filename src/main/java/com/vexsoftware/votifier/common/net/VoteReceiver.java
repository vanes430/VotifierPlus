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

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import com.vexsoftware.votifier.common.ForwardServer;
import com.vexsoftware.votifier.model.Vote;

public abstract class VoteReceiver extends Thread {

	private final String host;
	private final int port;

	private ServerSocket server;

	private volatile boolean running = true;

	private volatile VoteThrottleService throttleService;

	private volatile VoteForwarder voteForwarder;

	private volatile ExecutorService connectionExecutor;
	private volatile ExecutorService forwardExecutor;

	public VoteReceiver(String host, int port) throws Exception {
		super("Votifier I/O");
		this.host = host;
		this.port = port;
		setPriority(Thread.MIN_PRIORITY);
		initialize();
	}

	public ServerSocket getServer() { return server; }
	public VoteThrottleService getThrottleService() { return throttleService; }
	public VoteForwarder getVoteForwarder() { return voteForwarder; }

	public void initialize() throws Exception {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
			debug("Bound to " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
		} catch (Exception ex) {
			logSevere(
					"Error initializing vote receiver. Please verify that the configured IP address and port are not already in use.");
			ex.printStackTrace();
			throw new Exception(ex);
		}
	}

	public void shutdown() {
		running = false;

		if (server != null) {
			try {
				server.close();
			} catch (Exception ex) {
				logWarning("Unable to shut down vote receiver cleanly.");
			}
		}

		shutdownExecutor(connectionExecutor, "connection");
		shutdownExecutor(forwardExecutor, "forward");
	}

	private void shutdownExecutor(ExecutorService executor, String name) {
		if (executor == null) {
			return;
		}

		executor.shutdown();
		try {
			if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
					logWarning("Unable to fully shut down " + name + " executor.");
				}
			}
		} catch (InterruptedException ex) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public int getConnectionWorkerCount() {
		return 4;
	}

	public int getForwardWorkerCount() {
		return 1;
	}

	@Override
	public void run() {
		throttleService = new VoteThrottleService(getThrottleConfig());
		voteForwarder = new VoteForwarder(this);

		connectionExecutor = Executors.newFixedThreadPool(getConnectionWorkerCount(), new ThreadFactory() {
			private int id = 1;

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Votifier-Connection-" + id++);
				thread.setDaemon(true);
				return thread;
			}
		});

		forwardExecutor = Executors.newFixedThreadPool(getForwardWorkerCount(), new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Votifier-Forwarder");
				thread.setDaemon(true);
				return thread;
			}
		});

		final VoteConnectionHandler handler = new VoteConnectionHandler(this, throttleService);

		while (running) {
			try {
				final Socket socket = server.accept();

				connectionExecutor.submit(new Runnable() {
					@Override
					public void run() {
						try {
							Vote vote = handler.handle(socket);
							if (vote != null) {
								callEvent(vote);

								final Vote forwardVote = vote;
								forwardExecutor.submit(new Runnable() {
									@Override
									public void run() {
										try {
											voteForwarder.forwardVote(forwardVote);
										} catch (Exception ex) {
											logWarning("Error forwarding vote: "
													+ (ex.getLocalizedMessage() == null ? ex.getClass().getSimpleName()
															: ex.getLocalizedMessage()));
										}
									}
								});
							}
						} catch (Exception ex) {
							logWarning("Error processing vote connection: "
									+ (ex.getLocalizedMessage() == null ? ex.getClass().getSimpleName()
											: ex.getLocalizedMessage()));
						}
					}
				});
			} catch (SocketException ex) {
				if (running) {
					logWarning("Connection error while accepting vote socket: " + ex.getLocalizedMessage());
				} else {
					logWarning("Votifier socket closed.");
				}
			} catch (Exception ex) {
				logWarning("Error accepting vote connection: "
						+ (ex.getLocalizedMessage() == null ? ex.getClass().getSimpleName()
								: ex.getLocalizedMessage()));
			}
		}
	}

	public abstract boolean isUseTokens();

	public abstract ThrottleConfig getThrottleConfig();

	public abstract void logWarning(String warn);

	public abstract void logSevere(String msg);

	public abstract void log(String msg);

	public abstract void debug(String msg);

	public abstract void debug(Exception e);

	public abstract String getVersion();

	public abstract Set<String> getServers();

	public abstract KeyPair getKeyPair();

	public abstract Map<String, Key> getTokens();

	public abstract ForwardServer getServerData(String s);

	public abstract void callEvent(Vote e);

	public byte[] encrypt(byte[] data, PublicKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	public PublicKey getPublicKey(ForwardServer forwardServer) throws Exception {
		byte[] encoded = Base64.getDecoder().decode(forwardServer.getKey());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
	}

	public String getChallenge() {
		return com.vexsoftware.votifier.common.crypto.TokenUtil.newToken();
	}
}
