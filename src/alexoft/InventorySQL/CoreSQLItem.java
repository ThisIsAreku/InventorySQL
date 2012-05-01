package alexoft.InventorySQL;

import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CoreSQLItem {
	private Chest[] chests = null;
	private Player[] players = null;
	private CommandSender cs;

	public CoreSQLItem(Player[] players,
			CommandSender cs){
		this.players = players;
		this.cs = cs;
	}
	public CoreSQLItem(Chest[] chests,
			CommandSender cs){
		this.chests = chests;
		this.cs = cs;
	}
	public CoreSQLItem(Player[] players, Chest[] chests,
			CommandSender cs){
		this.players = players;
		this.chests = chests;
		this.cs = cs;
	}
	public CoreSQLItem(CommandSender cs){
		this.cs = cs;
	}
	public CommandSender getCommandSender(){
		return cs;
	}
	public Player[] getPlayers() {
		return this.players;
	}
	public Chest[] getChest() {
		return this.chests;
	}
	public boolean hasPlayersData(){
		if(this.players != null){
			return (this.players.length !=0);
		}
		return false;
	}
	public boolean hasChestData(){
		if(this.chests != null){
			return (this.chests.length !=0);
		}
		return false;
	}
	
	
}