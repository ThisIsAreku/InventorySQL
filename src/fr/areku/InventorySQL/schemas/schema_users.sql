ALTER IGNORE TABLE `%%TABLENAME%%` ADD `name` VARCHAR( 32 ) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%` ADD `password` VARCHAR( 32 ) NOT NULL AFTER `name`;
DELETE FROM `%%TABLENAME%%` USING `%%TABLENAME%%_users`, `%%TABLENAME%%_users` as vtable WHERE (NOT `%%TABLENAME%%_users`.ID=vtable.ID) AND (`%%TABLENAME%%_users`.name=vtable.name);
ALTER IGNORE TABLE `%%TABLENAME%%` ADD UNIQUE `name` ( `name` ) ;