<?php
/***********************
 * InventorySQL php example
 * require .jar version 0.3.2
 ***********************/
 
require 'mysql.inc.php';
require 'config.inc.php';
require 'items.inc.php';

$MySQL = new SQL($host, $user, $pass, $database);
$users = $MySQL->execute("SELECT `id`, `owner` FROM `".$table."`");
$data = true;
$inv_totalItems = 0;
$pend_totalItems = 0;
$inventory[] = array();
$pendings[] = array();
$tmp[] = array();
$index = 0;$isError = false;
$message = '';

if(isset($_GET['u'])){
	$data = $MySQL->execute("SELECT * FROM `".$table."` WHERE `id`='".$MySQL->escape($_GET['u'])."';");
}else{
	$data = $MySQL->execute("SELECT * FROM `".$table."`");
}

if(isset($_POST['give'])) {
	if($_POST['item_byte'] == '') $_POST['item_byte'] = 0;
	if($_POST['item_count'] == '') $_POST['item_count'] = 1;
	if(!is_numeric($_POST['item_id']) || !is_numeric($_POST['item_byte']) || !is_numeric($_POST['item_count'])) { $isError = true; $message = 'Please enter only numbers'; }
		$item_id = intval($_POST['item_id']);
		$item_byte = intval($_POST['item_byte']);
		$item_count = intval($_POST['item_count']);
	if($item_id == 0) { $isError = true; $message = 'Cannot give/remove AIR item !'; }
	if(!isset($_GET['u']) || !$data) { $isError = true; $message = 'Unknown id'; }
	if($item_count > 64) { $isError = true; $message = 'Too many items ! Limit is 64 per stack'; }
	if(!$isError) {
		$dat = '['.$_POST['item_action'].'('.$item_id.':'.$item_byte.')x'.$item_count.']';
		$give = $MySQL->execute("UPDATE `".$table."` SET `pendings`=CONCAT_WS(',',`pendings`,'".$MySQL->escape($dat)."') WHERE `id`='".$MySQL->escape($_GET['u'])."';");
	}
}

if(isset($_GET['u'])) {
	$data = $MySQL->execute("SELECT * FROM `".$table."` WHERE `id`='".$MySQL->escape($_GET['u'])."';");
	foreach (explode(",", $data[0]['inventory']) as $i) {
		if($i != '') {
		preg_match("/\[([0-9]{1,2})\(([0-9]{1,3}):([0-9]{1,2})\)x(-?[0-9]{1,2})\]/", $i, $matches);
				if($matches[2] != '0') {
					$inventory[$index] = array('slot' => intval($matches[1]), 'id' => intval($matches[2]), 'byte' => intval($matches[3]), 'number' => intval($matches[4]));
					$num = intval($matches[4]);
					if ($num > 0) $inv_totalItems += $num;
					if ($num < 0) $inv_totalItems += 1;
					$index++;
				}
		}
	}
	
	$index = 0;
	foreach (explode(",", $data[0]['pendings']) as $i) {
		if($i != '') {
		preg_match("/\[(-|\+)?\(([0-9]{1,3}):([0-9]{1,2})\)x([0-9]{1,2})\]/", $i, $matches);
				if($matches[2] != '0') {
					$pendings[$index] = array('param' => $matches[1], 'id' => intval($matches[2]), 'byte' => intval($matches[3]), 'number' => intval($matches[4])); 
					$num = intval($matches[4]);
					if ($num > 0) $pend_totalItems += $num;
					if ($num < 0) $pend_totalItems += 1;
					$index++;
				}
		}
	}
}

?><!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>InventorySQL</title>
<link href="css/default.css" rel="stylesheet" type="text/css" />
</head>

<body>
<form action="" method="get">
<fieldset>
<select name="u">
<?php 
foreach ($users as $user){
	echo '<option value="'.$user['id'].'"';
	if(isset($_GET['u'])) if($_GET['u'] == $user['id']) echo 'selected="selected"';
	echo '>'.$user['owner'].'</option>';
}
?>
</select>
<input type="submit" value="Change"/>
</fieldset>
</form>
<?php if(isset($_GET['u']))
	if($data) { ?>
<h3>Inventory</h3>
<div class="inventory-list">
<?php
echo $inv_totalItems." items in total<br />";
if($inv_totalItems > 0) {
	echo '<ul>';
	foreach($inventory as $inv) {
		echo '<li><b>Slot '.$inv['slot'].'</b> : '.$items[$inv['id']].':'.$inv['byte'].' (x'.$inv['number'].')';
	}
	echo '</ul>';
}else{
	echo '<i>No items</i>';
}
?>
</div>
<h3>Pendings</h3>
<div class="pendings-list">
<?php
echo $pend_totalItems." items in total<br />";
if($pend_totalItems > 0) {
	echo '<ul>';
	$txt = '';
	foreach($pendings as $pend) {
		if($pend['param'] == '+') {
			$txt = 'Add';
		}elseif($pend['param'] == '-'){
			$txt = 'Remove';
		}else{
			$txt = '(error..)';
		}
		echo '<li><b>'.$txt.'</b> : '.$items[$pend['id']].':'.$pend['byte'].' (x'.$pend['number'].')';
	}
	echo '</ul>';
}else{
	echo '<i>No items</i>';
}
?>
</div>
<form action="" method="post">
<fieldset>
<select name="item_action">
<option value="+">add</option>
<option value="-">remove</option>
</select>
<select name="item_id">
<?php 
foreach ($items as $id => $item){
	echo '<option value="'.$id.'">'.$item.'</option>';
}
?>
</select>:<input type="text" maxlength="2" name="item_byte" width="2" /> number : <input type="text" maxlength="2" name="item_count" width="2" /><br />
<input type="submit" value="Give" name="give"/> <?php if($isError) echo $message; ?>
</fieldset>
</form>
<?php
	}else{
		echo "<h2>Unknown id</h2>";
	} ?>
<footer>
Item list for minecraft <?php echo $items_version; ?>
</footer>
</body>
</html>