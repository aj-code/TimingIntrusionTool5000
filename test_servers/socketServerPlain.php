#!/usr/local/bin/php -q
<?php
//The code within this file is released into the public domain, see http://unlicense.org/.


error_reporting(E_ALL);

/* Allow the script to hang around waiting for connections. */
set_time_limit(0);





$socket = stream_socket_server("tcp://0.0.0.0:60000", $errno, $errstr);
if (!$socket) {
  echo "$errstr ($errno)<br />\n";
} else {
  while ($conn = stream_socket_accept($socket)) {
  	
  	$pwd = trim(fgets($conn));
  	
  	if ($pwd == 'qwerty')
    	fwrite($conn, 'SUCCESS');
    else
    	fwrite($conn, 'FAIL');
    
    fclose($conn);
  }
  fclose($socket);
}


?>
