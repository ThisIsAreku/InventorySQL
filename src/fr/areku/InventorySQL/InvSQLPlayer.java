package fr.areku.InventorySQL;

public class InvSQLPlayer {
	// private Player thePlayer;
	private String thePlayerName;
	private String theHash;
	private Long theEpoch;
	private boolean firstSessionCheck;

	public InvSQLPlayer(String playerName) {
		thePlayerName = playerName;
		theHash = "";
		theEpoch = 0L;
	}

	public InvSQLPlayer(String playerName, String hash, long epoch) {
		thePlayerName = playerName;
		theHash = hash;
		theEpoch = epoch;
		firstSessionCheck = true;
	}

	/*
	 * public Player getPlayer() { return thePlayer; }
	 */

	public String getPlayerName() {
		return thePlayerName;
	}

	public String getHash() {
		return theHash;
	}

	public Long getEpoch() {
		return theEpoch;
	}

	public boolean updateEpoch(Long newEpoch) {
		firstSessionCheck = false;
		if (newEpoch.equals(theEpoch)) {
			return false;
		} else {
			InventorySQL.d("New epoch for player " + thePlayerName + " : "
					+ newEpoch);
			theEpoch = newEpoch;
			return true;
		}
	}

	public boolean updateHash(String newHash) {
		if (newHash.equals(theHash)) {
			return false;
		} else {
			InventorySQL.d("New inv hash for player " + thePlayerName + " : "
					+ newHash);
			theHash = newHash;
			return true;
		}
	}

	public boolean isFirstCheck() {
		return (theEpoch == 0);
	}

	public boolean isFirstSessionCheck() {
		InventorySQL.d("isFirstSessionCheck:" + firstSessionCheck);
		return firstSessionCheck;
	}

}