ALTER IGNORE TABLE `%%TABLENAME%%` ADD `key` varchar(11) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `value` varchar(30) NOT NULL AFTER `key`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `is_backup` tinyint(4) NOT NULL AFTER `value`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD INDEX `id_is_backup` (`id`, `is_backup`), ADD INDEX `id` (`id`);