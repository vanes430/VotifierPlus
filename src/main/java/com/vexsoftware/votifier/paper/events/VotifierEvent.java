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
package com.vexsoftware.votifier.paper.events;

import org.bukkit.event.*;

import com.vexsoftware.votifier.model.Vote;

/**
 * {@code VotifierEvent} is a custom Bukkit event class that is sent
 * synchronously to CraftBukkit's main thread allowing other plugins to listener
 * for votes.
 *
 * @author frelling
 *
 */
public class VotifierEvent extends Event {
	/**
	 * Event listener handler list.
	 */
	private static final HandlerList handlers = new HandlerList();

	/**
	 * Encapsulated vote record.
	 */
	private Vote vote;

	/**
	 * Constructs a vote event that encapsulated the given vote record.
	 *
	 * @param vote
	 *            vote record
	 */
	public VotifierEvent(final Vote vote) {
		this.vote = vote;
	}

	/**
	 * Return the encapsulated vote record.
	 *
	 * @return vote record
	 */
	public Vote getVote() {
		return vote;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
