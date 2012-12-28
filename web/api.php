<?php
require 'config.inc.php';
require 'data.inc.php';

function generate_uuid()
{
    $md5 = md5(uniqid('', true));
    return substr($md5, 0, 8 ) . '-' .
            substr($md5, 8, 4) . '-' .
            substr($md5, 12, 4) . '-' .
            substr($md5, 16, 4) . '-' .
            substr($md5, 20, 12);
}

session_start();
header("Content-Type: application/json; charset=UTF-8");

$VERSION = "2.0";

$dbh = new PDO('mysql:host='.$invsql_mysqlhost.';dbname='.$invsql_mysqldb, $invsql_mysqluser, $invsql_mysqlpass, array(PDO::ATTR_PERSISTENT => true));
$dbh->exec("SET CHARACTER SET utf8");
if(!isset($_GET['p']))
{
	die(json_encode(array('success' => false)));
}
switch($_GET['p'])
{
	case 'versions':
		$r = $dbh->query("SHOW TABLE STATUS LIKE '".$invsql_tableprefix."_inventories'")->fetch();
		$plugin = explode(':', $r['Comment'], 2);
		echo json_encode(array('success' => true, 'plugin' => $plugin[1], 'web' => $VERSION));
	break;
	case 'items':
		echo json_encode(array('success' => true, 'items' => $items));
	break;
	case 'users':
		$q = $dbh->query('SELECT `id`, `name` FROM `'.$invsql_tableprefix.'_users`');
		$u = array();
		while($row = $q->fetch())
		{
			$u[$row['id']] = $row['name'];
		}
		echo json_encode(array('success' => true, 'users' => $u));
	break;
	case 'inv':
		$q = $dbh->prepare('SELECT
			`inventory`.`id` AS `p_id`,
			`inventory`.`item` AS `item`,
			`inventory`.`data` AS `data`,
			`inventory`.`damage` AS `damage`,
			`inventory`.`count` AS `count`,
			`inventory`.`date` AS `date`,
			`inventory`.`slot` AS `slot`,
			`enchantments`.`ench` AS `ench`,
			`enchantments`.`level` AS `level`,
			`meta`.`key` AS `meta_key`,
			`meta`.`value` AS `meta_value`
			FROM `'.$invsql_tableprefix.'_inventories` as `inventory`
			LEFT JOIN `'.$invsql_tableprefix.'_enchantments` AS `enchantments` ON `inventory`.`id` = `enchantments`.`id` AND `enchantments`.`is_backup` = 0
			LEFT JOIN `'.$invsql_tableprefix.'_meta` AS `meta` ON `inventory`.`id` = `meta`.`id` AND `meta`.`is_backup` = 0
			WHERE (`inventory`.`owner` = ? AND `inventory`.`world` = ?) ORDER BY `slot` ASC');
		$q->execute(array($_GET['u'], $_GET['w']));
		$inv = array();
		while($row = $q->fetch())
		{
			$s = $row['p_id'];
			if(!isset($inv[$s]))
			{
				$inv[$s] = array();
			}
			$inv[$s]['item'] = $row['item'];
			if(isset($items[$row['item']]))
			{
				$inv[$s]['item_name'] = $items[$row['item']];
			}else{
				$inv[$s]['item_name'] = "Unknown name";
			}
			$inv[$s]['slot'] = $row['slot'];
			$inv[$s]['data'] = $row['data'];
			$inv[$s]['damage'] = $row['damage'];
			$inv[$s]['count'] = $row['count'];
			$inv[$s]['date'] = $row['date'];
			
			if(isset($row['ench']))
			{
				if(!isset($inv[$s]['ench']))
				{
					$inv[$s]['ench'] = array();
				}
				$inv[$s]['ench'][] = array('id' => $row['ench'], 'level' => $row['level']);
			}
			if(isset($row['meta_key']))
			{
				if(!isset($inv[$s]['meta']))
				{
					$inv[$s]['meta'] = array();
				}
				$inv[$s]['meta'][] = array('key' => $row['meta_key'], 'value' => $row['meta_value']);
			}
		}
		echo json_encode(array('success' => true, 'inv' => $inv));		
	break;
	case 'pendings':
		$q = $dbh->prepare('SELECT
			`pendings`.`id` AS `p_id`,
			`pendings`.`item` AS `item`,
			`pendings`.`data` AS `data`,
			`pendings`.`damage` AS `damage`,
			`pendings`.`count` AS `count`,
			`enchantments`.`ench` AS `ench`,
			`enchantments`.`level` AS `level`,
			`meta`.`key` AS `meta_key`,
			`meta`.`value` AS `meta_value`
			FROM `'.$invsql_tableprefix.'_pendings` as `pendings`
			LEFT JOIN `'.$invsql_tableprefix.'_enchantments` AS `enchantments` ON `pendings`.`id` = `enchantments`.`id` AND `enchantments`.`is_backup` = 0
			LEFT JOIN `'.$invsql_tableprefix.'_meta` AS `meta` ON `pendings`.`id` = `meta`.`id` AND `meta`.`is_backup` = 0
			WHERE (`pendings`.`owner` = ? AND `pendings`.`world` = ?)');
		$q->execute(array($_GET['u'], $_GET['w']));
		$inv = array();
		while($row = $q->fetch())
		{
			$s = $row['p_id'];
			if(!isset($inv[$s]))
			{
				$inv[$s] = array();
			}
			$inv[$s]['item'] = $row['item'];
			if(isset($items[$row['item']]))
			{
				$inv[$s]['item_name'] = $items[$row['item']];
			}else{
				$inv[$s]['item_name'] = "Unknown name";
			}
			$inv[$s]['data'] = $row['data'];
			$inv[$s]['damage'] = $row['damage'];
			$inv[$s]['count'] = $row['count'];
			
			if(isset($row['ench']))
			{
				if(!isset($inv[$s]['ench']))
				{
					$inv[$s]['ench'] = array();
				}
				$inv[$s]['ench'][] = array('id' => $row['ench'], 'level' => $row['level']);
			}
			if(isset($row['meta_key']))
			{
				if(!isset($inv[$s]['meta']))
				{
					$inv[$s]['meta'] = array();
				}
				$inv[$s]['meta'][] = array('key' => $row['meta_key'], 'value' => $row['meta_value']);
			}
		}
		echo json_encode(array('success' => true, 'pendings' => $inv));
	break;
	case 'give':
		$uid = generate_uuid();
		$q = $dbh->prepare('INSERT INTO `'.$invsql_tableprefix.'_pendings` (`id` ,`owner` ,`world` ,`item` ,`data` ,`damage` ,`count`)VALUES (?,  ?,  ?,  ?,  ?,  ?,  ?);');
		$q->execute(array($uid, $_GET['u'], $_GET['w'], $_GET['item'], $_GET['data'], $_GET['damage'], $_GET['count']));
		$item = array(
		'name' => $items[$_GET['item']],
		'data' => $_GET['data'],
		'damage' => $_GET['damage'],
		'count' => $_GET['count'],
		'id' => $_GET['item']
		);
		echo json_encode(array('success' => true, 'item' => $item));		
}