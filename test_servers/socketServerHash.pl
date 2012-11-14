#The code within this file is released into the public domain, see http://unlicense.org/.

use IO::Socket;
use Digest::SHA  qw(sha1_hex);

$| = 1;
$socket = new IO::Socket::INET (
    LocalHost => '0.0.0.0',
    LocalPort => '60000',
    Proto => 'tcp',
    Listen => 5,
    Reuse => 1
);

while(1)
{
        $client_socket = $socket->accept();

        $client_socket->recv($password,10);
        $password =~ s/\s+$//;
        
        $hash = sha1_hex($password);
        
        if ($hash eq "b1b3773a05c0ed0176787a4f1574ff0075f7521e") {
		    $client_socket->send("Success");
		} else {
	        $client_socket->send("Fail");        
        }

        $client_socket->close();
        
}
