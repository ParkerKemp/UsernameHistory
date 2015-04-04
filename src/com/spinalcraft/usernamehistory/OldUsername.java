/* This file is part of UsernameHistory.
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

public class OldUsername implements Comparable<OldUsername> {
	String name;
	long changedToAt;

	public int compareTo(OldUsername other) {
		return Long.compare(this.changedToAt, other.changedToAt);
	}
}
