ALTER IGNORE TABLE `%%TABLENAME%%` ADD `owner` int(11) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `world` varchar(100) NOT NULL AFTER `owner`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `item` int(11) NOT NULL AFTER `world`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `data` tinyint(4) NOT NULL DEFAULT '0' AFTER `item`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `damage` smallint(6) NOT NULL DEFAULT '0' AFTER `data`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `count` int(11) NOT NULL AFTER `damage`;
ALTER IGNORE TABLE `%%TABLENAME%%` ENGINE = MYISAM