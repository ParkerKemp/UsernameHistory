package com.spinalcraft.usernamehistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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

class OldUsername implements Comparable<OldUsername> {
	String name;
	long changedToAt;

	public int compareTo(OldUsername other) {
		return Long.compare(this.changedToAt, other.changedToAt);
	}
}

public class UsernameHistory extends JavaPlugin implements Listener {
	ConsoleCommandSender console;
	private HashMap<String, OldUsername[]> cache = new HashMap<String, OldUsername[]>();

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
			getHistory(sender, args[0]);
			return true;
		}
		return false;
	}

	private void getHistory(CommandSender sender, String username) {
		OldUsername[] oldNames = cache.get(username.toLowerCase());
		if (oldNames != null) {
			reportHistory(sender, oldNames);
		} else {
			getHistoryFromWeb(sender, username);
		}
	}

	private void getHistoryFromWeb(final CommandSender sender,
			final String username) {

		new Thread() {
			public void run() {
				UUID uuid;
				String uuidString;
				Player player = Bukkit.getPlayer(username);
				if (player != null)
					uuidString = player.getUniqueId().toString();
				else {
					try {
						uuid = UUIDFetcher.getUUIDOf(username);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
					if (uuid != null)
						uuidString = uuid.toString();
					else {
						sender.sendMessage("");
						sender.sendMessage(ChatColor.RED
								+ "Player could not be found!");
						return;
					}
				}
				OldUsername[] names = usernames(uuidString, username);
				cache.put(username.toLowerCase(), names);
				reportHistory(sender, names);
			}
		}.start();
	}

	private void reportHistory(CommandSender sender, OldUsername[] names) {
		sender.sendMessage("");
		if (names.length == 1)
			sender.sendMessage(ChatColor.GREEN + names[0].name + ChatColor.GOLD
					+ " has never changed their name.");
		else {
			sender.sendMessage(ChatColor.GOLD + "Originally " + ChatColor.GREEN
					+ names[0].name);
			sender.sendMessage("");

			for (int i = 1; i < names.length; i++) {
				DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date date = new Date(names[i].changedToAt);
				String formattedDate = df.format(date);
				sender.sendMessage(ChatColor.BLUE + formattedDate
						+ ChatColor.GOLD + " - changed to " + ChatColor.GREEN
						+ names[i].name);
			}
		}
	}

	private OldUsername[] usernames(String uuid, String current) {
		Gson gson = new GsonBuilder().create();
		String compactUuid = uuid.replace("-", "");
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/"
					+ compactUuid + "/names");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			OldUsername[] oldNames = gson.fromJson(reader, OldUsername[].class);
			Arrays.sort(oldNames);
			reader.close();
			conn.disconnect();
			return oldNames;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
