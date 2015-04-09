/*
 * Copyright (c) 2015 Nate Mortensen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.spinalcraft.usernamehistory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

class FetchedUuid {
	String id;
}

/**
 * UUIDFetcher retrieves UUIDs from usernames via web requests to Mojang.
 * @author Parker Kemp
 *
 */
public class UUIDFetcher {
	private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
	private static HashMap<String, UUID> cache = new HashMap<String, UUID>();
	
	private static UUID fetch(String name) throws IOException {
		Gson gson = new GsonBuilder().create();
		UUID uuid = null;
		HttpURLConnection connection = createConnection();
		String body = gson.toJson(name);
		writeBody(connection, body);
		FetchedUuid[] id = gson.fromJson(
				new InputStreamReader(connection.getInputStream()),
				FetchedUuid[].class);
		if(id.length == 0)
			return null;
		uuid = UUIDFetcher.getUUID(id[0].id);
		cache.put(name.toLowerCase(), uuid);
		return uuid;
	}

	private static void writeBody(HttpURLConnection connection, String body)
			throws IOException {
		OutputStream stream = connection.getOutputStream();
		stream.write(body.getBytes());
		stream.flush();
		stream.close();
	}

	private static HttpURLConnection createConnection() throws IOException {
		URL url = new URL(PROFILE_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}

	private static UUID getUUID(String id) {
		return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12)
				+ "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-"
				+ id.substring(20, 32));
	}

	/**
	 * Returns the UUID of a player based on their current username.
	 * Results are cached locally (per session) to reduce web traffic.
	 * <p>
	 * <p>
	 * This method may make a synchronous web request. It is highly recommended to
	 * avoid running this on the main thread.
	 * @param name The username (case insensitive) to search
	 * @return The UUID of the given player, or null if player not found.
	 * @throws IOException
	 */
	public static UUID getUUIDOf(String name) throws IOException {
		UUID uuid = cache.get(name.toLowerCase());
		if(uuid == null)
			uuid = fetch(name);
		return uuid;
	}
}
