package com.spinalcraft.usernamehistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class SessionCache{
	private HashMap<String, History> usernameCache = new HashMap<String, History>();
	private HashMap<UUID, History> uuidCache = new HashMap<UUID, History>();
	
	public SessionCache(){
		
	}
	
	public History getFromUsername(String username){
		return usernameCache.get(username.toLowerCase());
	}
	
	public History getFromUuid(UUID uuid){
		return uuidCache.get(uuid);
	}
	
	public void putWithUsername(String username, History history){
		usernameCache.put(username.toLowerCase(), history);
	}
	
	public void putWithUuid(UUID uuid, History history){
		uuidCache.put(uuid, history);
	}
}

public class UHPlugin extends JavaPlugin implements Listener {
	ConsoleCommandSender console;

	private static SessionCache cache = new SessionCache();
	
	@Override
	public void onEnable() {
		console = Bukkit.getConsoleSender();

		console.sendMessage(ChatColor.BLUE + "UsernameHistory online!");

		getServer().getPluginManager().registerEvents((Listener) this, this);
	}

	@Override
	public void onDisable() {

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("original")) {
			if (args.length == 0)
				return false;
			reportHistory(sender, args[0]);
			return true;
		}
		return false;
	}

	private void reportHistory(CommandSender sender, String username) {
		History history = cache.getFromUsername(username.toLowerCase());
		if (history != null)
			printHistory(sender, history);
		else
			reportWebHistoryAsync(sender, username);
	}
	
	//For external use; does not fork
	public static History getHistoryFromUsername(String username){
		History history = cache.getFromUsername(username.toLowerCase());
		if(history != null)
			return history;
		else
			return getHistoryFromWeb(username);
	}
	
	//For external use; does not fork
	public static History getHistoryFromUuid(UUID uuid){
		History history = cache.getFromUuid(uuid);
		if(history != null)
			return history;
		else
			return webHistoryFromUuid(uuid);
	}

	private static History getHistoryFromWeb(String username) {
		UUID uuid;
		Player player = Bukkit.getPlayer(username);
		if (player != null)
			uuid = player.getUniqueId();
		else {
			try {
				uuid = UUIDFetcher.getUUIDOf(username);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if (uuid == null)
				return null;
		}
		History history = webHistoryFromUuid(uuid);
		cache.putWithUsername(username.toLowerCase(), history);
		return history;
	}
	
	private void reportWebHistoryAsync(final CommandSender sender, final String username){
		new Thread() {
			public void run() {
				History history = getHistoryFromWeb(username);
				if(history == null){
					sender.sendMessage("");
					sender.sendMessage(ChatColor.RED + "Player could not be found!");
					return;
				}
				printHistory(sender, history);
			}
		}.start();
	}

	private void printHistory(CommandSender sender, History history) {
		sender.sendMessage("");
		if (history.oldNames.length == 1)
			sender.sendMessage(ChatColor.GREEN + history.oldNames[0].name + ChatColor.GOLD
					+ " has never changed their name.");
		else {
			sender.sendMessage(ChatColor.GOLD + "Originally " + ChatColor.GREEN
					+ history.oldNames[0].name);
			sender.sendMessage("");

			for (int i = 1; i < history.oldNames.length; i++) {
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date date = new Date(history.oldNames[i].changedToAt);
				String formattedDate = df.format(date);
				sender.sendMessage(ChatColor.BLUE + formattedDate
						+ ChatColor.GOLD + " - changed to " + ChatColor.GREEN
						+ history.oldNames[i].name);
			}
		}
	}
	
	private static History webHistoryFromUuid(UUID uuid){
		Gson gson = new GsonBuilder().create();
		String compactUuid = uuid.toString().replace("-", "");
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/"
					+ compactUuid + "/names");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			OldUsername[] oldNames = gson.fromJson(reader, OldUsername[].class);
			reader.close();
			conn.disconnect();
			History history = new History(uuid, oldNames);
			cache.putWithUuid(uuid, history);
			return new History(uuid, oldNames);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
