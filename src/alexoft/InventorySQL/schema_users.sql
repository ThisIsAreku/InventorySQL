ALTER IGNORE TABLE `%%TABLENAME%%_users` ADD `name` VARCHAR( 32 ) NOT NULL AFTER `id`;
ALTER IGNORE TABLE `%%TABLENAME%%_users` ADD `password` VARCHAR( 32 ) NOT NULL AFTER `name`;
DELETE FROM `%%TABLENAME%%_users` USING `%%TABLENAME%%_users`, `%%TABLENAME%%_users` as vtable WHERE (NOT `%%TABLENAME%%_users`.ID=vtable.ID) AND (`%%TABLENAME%%_users`.name=vtable.name);
ALTER IGNORE TABLE `%%TABLENAME%%_users` ADD UNIQUE `name` ( `name` ) ;