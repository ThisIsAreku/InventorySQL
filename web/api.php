<?php
require 'config.inc.php';
require 'data.inc.php';

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
	case 'users':
		$q = $dbh->query('SELECT `id`, `name` FROM `'.$invsql_tableprefix.'_users`');
		$u = array();
		while($row = $q->fetch())
		{
			$u[$row['id']] = $row['name'];
		}
		echo json_encode(array('success' => true, 'users' => $u));
	break;
}