package com.spinalcraft.usernamehistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class OldUsername{
	String name;
	long changedToAt;
}

public class UsernameHistory extends JavaPlugin implements Listener{
	ConsoleCommandSender console;
	
	@Override
	public void onEnable(){
		console = Bukkit.getConsoleSender();
		
		console.sendMessage(code(Color.BLUE) + "UsernameHistory online!");
		
		getServer().getPluginManager().registerEvents((Listener)this,  this);
	}
	
	@Override
	public void onDisable(){
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("original")) {
			if(args.length == 0)
				return false;
			getHistoryAsync(sender, args[0]);
			return true;
		}
		return false;
	}

	private void getHistoryAsync(final CommandSender sender,
			final String username) {
		
		new Thread() {
			public void run() {
				String uuid;
				Player player = Bukkit.getPlayer(username);
				if (player != null)
					uuid = player.getUniqueId().toString();
				else
					try {
						uuid = UUIDFetcher.getUUIDOf(username).toString();
					} catch (Exception e) {
						sender.sendMessage("");
						sender.sendMessage(code(Color.RED)
								+ "Player could not be found!");
						return;
					}
				ArrayList<OldUsername> names = usernames(uuid, username);
				reportHistory(sender, names);
			}
		}.start();
	}
	
	private void reportHistory(CommandSender sender, ArrayList<OldUsername> names){
		sender.sendMessage("");
		if(names.size() == 1)
			sender.sendMessage(code(Color.GREEN) + names.get(0) + code(Color.GOLD) + " has never changed their name.");
		else{
			sender.sendMessage(code(Color.GOLD) + "Originally " + code(Color.GREEN) + names.get(0).name);
			sender.sendMessage("");
			
			for(int i = 1; i < names.size(); i++){
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date date = new Date(names.get(i).changedToAt);
				String formattedDate = df.format(date);
				sender.sendMessage(code(Color.BLUE) + formattedDate + code(Color.GOLD) + " - changed to " + code(Color.GREEN) +  names.get(i).name);
			}
		}
	}

	private ArrayList<OldUsername> usernames(String uuid, String current){
		ArrayList<OldUsername> nameList = new ArrayList<OldUsername>();
		Gson gson = new GsonBuilder().create();
		String compactUuid = uuid.replace("-", "");
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/" + compactUuid + "/names");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			OldUsername[] oldNames = gson.fromJson(reader, OldUsername[].class);
			
			//HACK: I don't trust Mojang to always give the original name first, so I re-order it myself
			for(int i = 0; i < oldNames.length; i++){
				if(oldNames[i].changedToAt == 0)
					nameList.add(oldNames[i]);
			}
			for(int i = 0; i < oldNames.length; i++){
				if(oldNames[i].changedToAt != 0)
					nameList.add(oldNames[i]);
			}
			reader.close();
			conn.disconnect();
			return nameList;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String code(Color color){
		switch(color){
		case BLACK:
			return "\u00A70";
		case DARKBLUE:
			return "\u00A71";
		case DARKGREEN:
			return "\u00A72";
		case DARKAQUA:
			return "\u00A73";
		case DARKRED:
			return "\u00A74";
		case DARKPURPLE:
			return "\u00A75";
		case GOLD:
			return "\u00A76";
		case GRAY:
			return "\u00A77";
		case DARKGRAY:
			return "\u00A78";
		case BLUE:
			return "\u00A79";
		case GREEN:
			return "\u00A7a";
		case AQUA:
			return "\u00A7b";
		case RED:
			return "\u00A7c";
		case LIGHTPURPLE:
			return "\u00A7d";
		case YELLOW:
			return "\u00A7e";
		case WHITE:
			return "\u00A7f";
		}
		return "";
	}
}
