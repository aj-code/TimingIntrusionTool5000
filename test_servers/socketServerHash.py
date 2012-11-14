#!/usr/bin/env python
#The code within this file is released into the public domain, see http://unlicense.org/.

import socket
import hashlib
import binascii

host = '0.0.0.0'
port = 60000
backlog = 5
size = 1024
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind((host,port))
s.listen(backlog)


try:
	while 1:

		client, address = s.accept()
		data = client.recv(size).rstrip()

		hash = hashlib.sha1(data).hexdigest();
		
#		print("%s : %s" % (data, hash))
		if hash == '038cba2fbdd1cdc8209136e9df8b26fd007e371c': #mcartney
			client.send('true')
		else:
			client.send('false')

		client.close()

except Exception, err:
    print(err)
    s.close()


