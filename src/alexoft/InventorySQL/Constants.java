package alexoft.InventorySQL;

public final class Constants {
	public static final String REQ_CLEANUP_BACKUP = "DELETE FROM `%s_backup` WHERE `timestamp` < (CURRENT_TIMESTAMP - INTERVAL '%s' DAY);";
	public static final String REQ_INSERT_BACKUP = "INSERT INTO `%s_backup` (`id`, `owner`, `world`, `x`, `y`, `z`, `inventory`, `timestamp`) VALUES (NULL, '%s', '%s', '%s', '%s', '%s', '%s', CURRENT_TIMESTAMP);";
	
	public static final String REQ_INSERT_USER = "INSERT INTO `%s_users`(`name`, `password`) VALUES ('%s', '') ON DUPLICATE KEY UPDATE `password`=`password`;";
	
	public static final String REQ_INSERT_INV = "INSERT INTO `%s`(`owner`, `world`, `inventory`, `pendings`, `x`, `y`, `z`) VALUES ('%s','%s','%s','','%s','%s','%s');";
	public static final String REQ_UPDATE_INV = "UPDATE `%s` SET `inventory` = '%s', `pendings` = '%s', `x`= '%s', `y`= '%s', `z`= '%s' WHERE `id`= '%s';";
}
