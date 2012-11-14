#The code within this file is released into the public domain, see http://unlicense.org/.

import tornado.ioloop
import tornado.web
import hashlib
import binascii

class MainHandler(tornado.web.RequestHandler):
	def get(self):
		self.write("Hello, world")
		
	def post(self):
		pwd = self.request.arguments['password'][0]
		
		hash = hashlib.sha1(pwd).hexdigest();
		
		#print("%s : %s" % (pwd, hash))
		if hash == '038cba2fbdd1cdc8209136e9df8b26fd007e371c': #mcartney
			self.write('true')
		else:
			self.write('false')


application = tornado.web.Application([
	(r"/", MainHandler),
])

if __name__ == "__main__":
	application.listen(8888)
	tornado.ioloop.IOLoop.instance().start()
