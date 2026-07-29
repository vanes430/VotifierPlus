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
package com.vexsoftware.votifier.model;

public class Vote {

	private String serviceName;
	private String username;
	private String address;
	private String timeStamp;
	private String sourceAddress;

	public Vote(String serviceName, String username, String address, String timeStamp) {
		this.serviceName = serviceName;
		this.username = username;
		this.address = address;
		this.timeStamp = timeStamp;
	}

	public Vote() {
	}

	@Override
	public String toString() {
		return "Vote (from:" + serviceName + " username:" + username + " address:" + address + " timeStamp:" + timeStamp
				+ ", sourceAddress:" + sourceAddress + ")";
	}

	public void setServiceName(String serviceName) { this.serviceName = serviceName; }
	public String getServiceName() { return serviceName; }

	public void setUsername(String username) { this.username = username.length() <= 16 ? username : username.substring(0, 16); }
	public String getUsername() { return username; }

	public void setAddress(String address) { this.address = address; }
	public String getAddress() { return address; }

	public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }
	public String getTimeStamp() { return timeStamp; }

	public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }
	public String getSourceAddress() { return sourceAddress; }
}
