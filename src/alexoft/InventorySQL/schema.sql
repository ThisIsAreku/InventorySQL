CREATE TABLE IF NOT EXISTS `%%TABLENAME%%` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner` varchar(32) NOT NULL,
  `ischest` tinyint(1) NOT NULL DEFAULT '0',
  `x` int(11) NOT NULL DEFAULT '0',
  `y` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `z` int(11) NOT NULL DEFAULT '0',
  `inventory` longtext,
  `pendings` longtext,
  `last_update` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;