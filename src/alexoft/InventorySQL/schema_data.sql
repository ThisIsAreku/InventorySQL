ALTER IGNORE TABLE `%%TABLENAME%%` ADD `owner` VARCHAR( 32 ) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `world` VARCHAR( 254 ) NOT NULL AFTER `owner`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `ischest` TINYINT( 1 ) NOT NULL AFTER `world`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `x` INT( 11 ) NOT NULL DEFAULT '0' AFTER `ischest`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `y` INT( 11 ) NOT NULL DEFAULT '0' AFTER `x`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `z` INT( 11 ) NOT NULL DEFAULT '0' AFTER `y`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `inventory` LONGTEXT AFTER `z`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `pendings` LONGTEXT AFTER `inventory`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `last_update` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP AFTER `pendings`;
ALTER IGNORE TABLE `%%TABLENAME%%` CHANGE `ischest` `ischest` TINYINT( 1 ) NOT NULL DEFAULT '0';
ALTER IGNORE TABLE `%%TABLENAME%%` CHANGE `y` `y` INT( 11 ) NOT NULL DEFAULT '0';
ALTER TABLE `inventorysql` CHANGE `owner` `owner` VARCHAR( 32 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL;