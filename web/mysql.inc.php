<?php
/* SQL class - @author ThisIsAreku */
class SQL {
	var $_VERSION = "1.2";
	
	var $host;
	var $user;
	var $pass;
	var $database;
	
	var $con;
	
	function SQL($host, $user, $pass, $database) {
		$this->host = $host;
		$this->pass = $pass;
		$this->user = $user;
		$this->database = $database;
		$this->__connect();
	}
	private function __connect() {
		$this->con = mysql_connect($this->host, $this->user, $this->pass);
		mysql_select_db($this->database, $this->con);
		mysql_query("SET NAMES 'utf8'", $this->con);
	}
	private function __compile($query) {
		if ($query === false || $query === true) return $query;
		$out = array();
		$id = 0;
		while($row = mysql_fetch_assoc($query)) {
			$out[$id] = $row;
			$id += 1;
		}
		return $out;
	}
	private function __escape($d) {
		if(is_array($d))
			foreach($d as $k => $v)
				$d[$k] = $this->__escape($v); 
		elseif(is_string($d))
			$d = mysql_real_escape_string($d, $this->con);
		return $d;
	}
	
	
	
	public function escape($data) {
		return $this->__escape($data);
	}
	
	public function last_error() {
		return mysql_error($this->con);
	}
	
	public function showTable($table, $columns = null, $more = null){
		$sql = "SELECT ";
		if(!$columns == null)  {
			foreach($columns as $c) {
				$sql .= "`" . $c . "`, ";
			}
			$sql = substr($sql, 0, (strlen($sql) - 2));
		}else{
			$sql .= "*";
		}
		$sql .= " FROM `".$table."`";
		if(!$more == null) $sql .= ' '.$more;
		$query = mysql_query($sql, $this->con);
		return $this->__compile($query);
	}
	
	public function getColumn($result_array, $row) {
		$r = array();
		for($i=0; $i < count($result_array); $i++) {
			$r[$i] = $result_array[$i][$row];
		}
		return $r;
	}
	
	public function execute($sql){
		$query = mysql_query($sql, $this->con);
		return $this->__compile($query);
	}
}