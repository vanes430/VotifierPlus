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
package com.vexsoftware.votifier.model;

import com.vexsoftware.votifier.model.Vote;

/**
 * Backward-compatible Votifier event.
 * Old plugins listening for com.vexsoftware.votifier.model.VotifierEvent
 * still work — this extends the canonical paper.events.VotifierEvent.
 *
 * New plugins may listen for either this class or paper.events.VotifierEvent.
 */
public class VotifierEvent extends com.vexsoftware.votifier.paper.events.VotifierEvent {

	private static final long serialVersionUID = 1L;

	public VotifierEvent(Vote vote) {
		super(vote);
	}
}
