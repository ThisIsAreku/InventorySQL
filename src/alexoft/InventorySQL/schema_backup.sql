ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `owner` varchar(32) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `world` varchar(254) NOT NULL AFTER `owner`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `x` int(11) NOT NULL DEFAULT '0' AFTER `world`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `y` int(11) NOT NULL DEFAULT '0' AFTER `x`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `z` int(11) NOT NULL DEFAULT '0' AFTER `y`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `inventory` LONGTEXT AFTER `z`;
ALTER IGNORE TABLE `%%TABLENAME%%_backup` ADD `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `inventory`;
