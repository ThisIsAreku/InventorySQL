package fr.areku.InventorySQL.database;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.areku.InventorySQL.Main;

public class PlayersInventoryHash {
	private Map<String, String> trackPlayerInvModified;
	private Map<String, Long> trackPlayerInvLastCheck;
	private YamlConfiguration filedb;
	private File f;
	static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public PlayersInventoryHash(File base) {
		trackPlayerInvModified = new HashMap<String, String>();
		trackPlayerInvLastCheck = new HashMap<String, Long>();
		f = new File(base, "invhash.yml");
		loadFromFile();
	}

	public static ItemStack[] concatItemStack(ItemStack[] A, ItemStack[] B) {
		ItemStack[] C = new ItemStack[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);

		return C;
	}

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

	public void sync(Player p) {
		if (!(this.trackPlayerInvLastCheck.containsKey(p.getName()) && this.trackPlayerInvModified
				.containsKey(p.getName()))) {
			this.trackPlayerInvLastCheck.put(p.getName(), 0L);
			this.trackPlayerInvModified.put(p.getName(), "");
		}
	}

	public boolean isPlayerInventoryModified(Player player) {
		sync(player);
		String newHash = computePlayerInventoryHash(player);
		if (newHash.equals(this.trackPlayerInvModified.get(player.getName()))) {
			return false;
		} else {
			Main.d("New inv hash for player " + player.getName() + " : "
					+ newHash);
			updateEntryOf(player.getName(), newHash, null);
			this.trackPlayerInvModified.put(player.getName(), newHash);
			return true;
		}

	}

	public long getPlayerLastCheck(Player player) {
		sync(player);
		return this.trackPlayerInvLastCheck.get(player.getName());
	}

	public void updatePlayerLastCheck(Player player, long newEpoch) {
		Main.d("Updated last check for Player " + player.getName() + " to "
				+ newEpoch);
		updateEntryOf(player.getName(), null, newEpoch);
		this.trackPlayerInvLastCheck.put(player.getName(), newEpoch);
	}

	private void updateEntryOf(String k, String v, Long epoch) {
		if (v != null)
			filedb.set(k + ".hash", v);
		if (epoch != null)
			filedb.set(k + ".epoch", epoch);
		try {
			filedb.save(f);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void writeToFile() {
		for (Entry<String, String> e : this.trackPlayerInvModified.entrySet()) {
			filedb.set(e.getKey() + ".hash", e.getValue());
			filedb.set(e.getKey() + ".epoch",
					this.trackPlayerInvLastCheck.get(e.getKey()));
		}
		try {
			filedb.save(f);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void loadFromFile() {
		filedb = new YamlConfiguration();
		if (f.exists()) {
			try {
				filedb.load(f);
				for (Entry<String, Object> e : filedb.getValues(false)
						.entrySet()) {
					MemorySection ms = (MemorySection) e.getValue();

					this.trackPlayerInvModified.put(e.getKey(),
							ms.getString("hash"));
					this.trackPlayerInvLastCheck.put(e.getKey(),
							ms.getLong("epoch"));
				}
				Main.d("Loaded " + this.trackPlayerInvModified.size()
						+ " hashes from file");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
