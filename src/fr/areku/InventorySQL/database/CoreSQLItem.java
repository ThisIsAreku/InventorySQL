package fr.areku.InventorySQL.database;

import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoreSQLItem {
	private Chest[] chests = null;
	private Player[] players = null;
	private CommandSender cs = null;

	public CoreSQLItem setCommandSender(CommandSender cs) {
		this.cs = cs;
		return this;
	}

	public CoreSQLItem(Player[] players) {
		this.players = players;
	}

	public CoreSQLItem(Chest[] chests) {
		this.chests = chests;
	}

	public CoreSQLItem(Player[] players, Chest[] chests) {
		this.players = players;
		this.chests = chests;
	}

	public Player[] getPlayers() {
		return this.players;
	}

	public Chest[] getChest() {
		return this.chests;
	}

	public CommandSender getCommandSender() {
		return this.cs;
	}
	
	public void sendMessage(String msg){
		if (this.cs != null) {
			this.cs.sendMessage(msg);
		}
	}

	public boolean hasPlayersData() {
		if (this.players != null) {
			return (this.players.length != 0);
		}
		return false;
	}

	public boolean hasChestData() {
		if (this.chests != null) {
			return (this.chests.length != 0);
		}
		return false;
	}

}
