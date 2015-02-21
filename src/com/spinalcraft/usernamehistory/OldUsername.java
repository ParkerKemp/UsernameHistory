package com.spinalcraft.usernamehistory;

public class OldUsername implements Comparable<OldUsername> {
	String name;
	long changedToAt;

	public int compareTo(OldUsername other) {
		return Long.compare(this.changedToAt, other.changedToAt);
	}
}
