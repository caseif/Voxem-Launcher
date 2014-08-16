<?php
error_reporting(E_NOTICE ^ E_ALL);
$versionFile = file_get_contents("version");
$versionLines = explode("\n", $versionFile);
$versionStage = explode(": ", $versionLines[0])[1];
$versionNum = explode(": ", $versionLines[1])[1];
$versionString = $versionStage." ".$versionNum;
require("connect.php");
mysql_query("INSERT INTO downloads (ip, time, version) VALUES ('".$_SERVER['REMOTE_ADDR']."', '".time()."', '".$versionString."')");
mysql_close();
$fullpath = "C:\WebServer\mineflat\MineFlat.jar";
header("Cache-Control: public, must-revalidate");
header("Pragma: hack");
header("Content-Type: application/octet-stream");
header("Content-Length: ".(string)(filesize($fullpath)));
header("Content-Disposition: attachment; filename='MineFlat.jar'");
header("Content-Transfer-Encoding: binary\n");
ob_end_clean();
readfile($fullpath);
?>