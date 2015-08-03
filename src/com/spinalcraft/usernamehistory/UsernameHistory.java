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
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class SessionCache{
	private static HashMap<String, UHistory> usernameCache = new HashMap<String, UHistory>();
	private static HashMap<UUID, UHistory> uuidCache = new HashMap<UUID, UHistory>();
	
	public static UHistory getFromUsername(String username){
		return usernameCache.get(username.toLowerCase());
	}
	
	public static UHistory getFromUuid(UUID uuid){
		return uuidCache.get(uuid);
	}
	
	public static void putWithUsername(String username, UHistory history){
		usernameCache.put(username.toLowerCase(), history);
	}
	
	public static void putWithUuid(UUID uuid, UHistory history){
		uuidCache.put(uuid, history);
	}
}

public class UsernameHistory extends JavaPlugin {
	ConsoleCommandSender console;
	
	@Override
	public void onEnable() {
		console = Bukkit.getConsoleSender();

		console.sendMessage(ChatColor.BLUE + "UsernameHistory online!");
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
		UHistory history = SessionCache.getFromUsername(username.toLowerCase());
		if (history != null)
			printHistory(sender, history);
		else
			reportWebHistoryAsync(sender, username);
	}
	
	/**
	 * Returns a {@link UHistory} object containing a list of prior names for a player.
	 * Results are cached locally (per session) to reduce web traffic.
	 * <p>
	 * <p>
	 * This method may make a synchronous web request. It is highly recommended to
	 * avoid running this on the main thread.
	 * @param username The username of the player
	 * @return The UHistory of the specified player, or null if player not found.
	 * @see #getHistoryFromUuid(UUID)
	 */
	public static UHistory getHistoryFromUsername(String username){
		UHistory history = SessionCache.getFromUsername(username.toLowerCase());
		if(history != null)
			return history;
		else
			return getHistoryFromWeb(username);
	}
	
	/**
	 * Returns a {@link UHistory} object containing a list of prior names for a player.
	 * Results are cached locally (per session) to reduce web traffic.
	 * <p>
	 * <p>
	 * This method may make a synchronous web request. It is highly recommended to
	 * avoid running this on the main thread.
	 * @param uuid The UUID of the player
	 * @return The UHistory of the specified player, or null if player not found.
	 * @see #getHistoryFromUsername(String)
	 * @see UUIDFetcher
	 */
	public static UHistory getHistoryFromUuid(UUID uuid){
		UHistory history = SessionCache.getFromUuid(uuid);
		if(history != null)
			return history;
		else
			return webHistoryFromUuid(uuid);
	}

	@SuppressWarnings("deprecation")
	private static UHistory getHistoryFromWeb(String username) {
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
		UHistory history = webHistoryFromUuid(uuid);
		SessionCache.putWithUsername(username.toLowerCase(), history);
		return history;
	}
	
	private void reportWebHistoryAsync(final CommandSender sender, final String username){
		new Thread() {
			public void run() {
				UHistory history = getHistoryFromWeb(username);
				if(history == null){
					sender.sendMessage(ChatColor.RED + "Player could not be found!");
					return;
				}
				printHistory(sender, history);
			}
		}.start();
	}

	private void printHistory(CommandSender sender, UHistory history) {
		UName[] oldNames = history.getOldUsernames();
		if (oldNames.length == 1)
			sender.sendMessage(ChatColor.GREEN + oldNames[0].getName()+ ChatColor.GOLD + " has never changed their name.");
		else {
			sender.sendMessage(ChatColor.GOLD + "Original name: " + ChatColor.GREEN + oldNames[0].getName());

			for (int i = 1; i < oldNames.length; i++) {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = new Date(oldNames[i].getTimeChanged());
				String formattedDate = df.format(date);
				sender.sendMessage(ChatColor.BLUE + formattedDate + ChatColor.GOLD + " changed to " + ChatColor.GREEN +  oldNames[i].getName());
			}
		}
	}
	
	private static UHistory webHistoryFromUuid(UUID uuid){
		Gson gson = new GsonBuilder().create();
		String compactUuid = uuid.toString().replace("-", "");
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/"
					+ compactUuid + "/names");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			UName[] oldNames = gson.fromJson(reader, UName[].class);
			if(oldNames == null)
				return null;
			reader.close();
			conn.disconnect();
			UHistory history = new UHistory(uuid, oldNames);
			SessionCache.putWithUuid(uuid, history);
			return new UHistory(uuid, oldNames);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
