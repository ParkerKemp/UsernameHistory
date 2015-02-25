package com.spinalcraft.usernamehistory;

import java.util.Arrays;
import java.util.UUID;

public class History{
	UUID uuid;
	
	OldUsername[] oldNames;
	
	public History(UUID uuid, OldUsername[] oldNames){
		this.uuid = uuid;
		this.oldNames = oldNames;
		Arrays.sort(this.oldNames);
	}
}
