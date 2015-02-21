package com.spinalcraft.usernamehistory;

import java.util.Arrays;

public class History{
	String uuid;
	
	OldUsername[] oldNames;
	
	public History(String uuid, OldUsername[] oldNames){
		this.uuid = uuid;
		this.oldNames = oldNames;
		Arrays.sort(this.oldNames);
	}
}
