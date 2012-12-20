package fr.areku.InventorySQL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerManager {
	private static PlayerManager instance;
	private Map<String, InvSQLPlayer> playerMap = null;
	private File f;

	public PlayerManager(File dataFile) {
		playerMap = new HashMap<String, InvSQLPlayer>();
		f = dataFile;
		if (f.exists()) {
			loadDatas();
		}
		instance = this;
	}

	public static PlayerManager getInstance() {
		return instance;
	}

	public static int getNumPlayers() {
		return instance.playerMap.size();
	}

	public void saveDatas() {
		String outStr = "";
		for (Entry<String, InvSQLPlayer> e : playerMap.entrySet()) {
			outStr += e.getKey() + ':' + e.getValue().getHash() + ':'
					+ e.getValue().getEpoch()
					+ System.getProperty("line.separator");

		}
		try {
			FileOutputStream fstream = new FileOutputStream(f);
			DataOutputStream out = new DataOutputStream(fstream);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write(outStr.trim());
			bw.flush();
			out.close();
			InventorySQL.d("Data Saved");
		} catch (IOException e) {
			InventorySQL.logException(e, "Error while saving data cache");
		}
	}

	public void loadDatas() {
		try {
			FileInputStream fstream = new FileInputStream(f);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			String[] spl;

			playerMap.clear();
			while ((strLine = br.readLine()) != null) {
				spl = strLine.split(":");
				playerMap
						.put(spl[0],
								new InvSQLPlayer(spl[0], spl[1], Long
										.parseLong(spl[2])));
			}
			in.close();
			InventorySQL.d("Data Loaded, " + playerMap.size());
		} catch (Exception e) {
			InventorySQL.logException(e, "Error while loading data cache");
		}
	}

	public InvSQLPlayer get(String name) {
		if (!playerMap.containsKey(name)) {
			playerMap.put(name, new InvSQLPlayer(name));
		}
		return playerMap.get(name);
	}

	/*
	 * public void onPlayerLogin(Player p){ if
	 * (!playerMap.containsKey(p.getName())) { InvSQLPlayer pl = new
	 * InvSQLPlayer(Bukkit.getPlayerExact(p.getName()); pl.
	 * playerMap.put(p.getName(), )); } }
	 */

	public static ItemStack[] concatItemStack(ItemStack[] A, ItemStack[] B) {
		ItemStack[] C = new ItemStack[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);

		return C;
	}

	static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public static String computePlayerInventoryHash(Player player) {
		ItemStack[] all = concatItemStack(player.getInventory()
				.getArmorContents(), player.getInventory().getContents());

		StringBuilder serialized = new StringBuilder();
		for (ItemStack s : all) {
			if (s == null)
				continue;
			for (Entry<String, Object> e : s.serialize().entrySet())
				serialized.append(e.getKey() + " => " + e.getValue());
		}
		serialized.append(InventorySQL.getVersion());
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(serialized.toString().getBytes("UTF-8"));
			byte[] hash = digest.digest();

			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
				sb.append(HEX_CHARS[b & 0x0F]);
			}
			String hex = sb.toString();
			return hex;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
