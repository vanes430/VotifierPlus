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
package com.vexsoftware.votifier.velocity.event;

import com.velocitypowered.api.event.ResultedEvent;
import com.vexsoftware.votifier.model.Vote;

public class VotifierEvent implements ResultedEvent<ResultedEvent.GenericResult> {
	private final Vote vote;
	private GenericResult result;

	public VotifierEvent(Vote vote) {
		this.vote = vote;
		this.result = GenericResult.allowed();
	}

	public Vote getVote() {
		return vote;
	}

	@Override
	public GenericResult getResult() {
		return this.result;
	}

	@Override
	public void setResult(GenericResult result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "VotifierEvent{" + "vote=" + vote + '}';
	}
}
