/* 
 * Copyright (c) 2015 Parker Kemp
 * 
 * This file is part of UsernameHistory.
 *
 * UsernameHistory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UsernameHistory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with UsernameHistory.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.spinalcraft.usernamehistory;

/**
 * UName contains a single username, prior or current, in the user's history, along with the time it was changed.
 * @author Parker Kemp
 */
public class UName implements Comparable<UName> {
	private String name;
	private long changedToAt;

	public int compareTo(UName other) {
		return Long.compare(this.changedToAt, other.changedToAt);
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * Returns the UNIX timestamp at which this username was created. For the original username, returns zero.
	 * @return The time changed.
	 */
	public long getTimeChanged(){
		return changedToAt;
	}
}
