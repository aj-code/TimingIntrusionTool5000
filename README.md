TimingIntrusionTool5000
=======================

TimingIntrusionTool5000 is a tool for performing timing attacks on plaintext and hashed network password authentication. A novel technique is employed to infer the prefix of a hash of a user's password. This prefix can then be used to eliminate passwords from a standard wordlist and fall back to a good ol' fashioned brute force (with hydra or whatever). With large passwords lists (over 100 million words) this can significantly speed up the attack on vulnerable servers over fast networks.

More info is in my [Kiwicon 666 Presentation](http://security-assessment.com/files/documents/presentations/TimingAttackPresentation2012.pdf).

Ideas and contributions are welcome, email me at <aj@shinynightmares.com>.


Features
--------

* Jitter filtering based on 10th percentile after multiple measurements.
* Accurate cross-platform timing (probably).
* Socket tuning, sending, receiving.
* Hash prefix collision generation.
* Statistical calculations including automatic winner classification.
* Multithreaded wordlist reduction and attacks.

Limitations
----------

* This will not work with most network servers. Use test mode to find out. Generally if the server is doing too much stuff on a failed login this will fail, also it's language dependant. Python seems to be most vulnerable, Java least.
* This will not work on salted hashes.
* A full plaintext attack is not implemented (send me a patch or pull request).
* Only hex encoded hashes are supported.
* Untested on slow networks (ie the internet).


Use
---

java -jar TimingIntrusionTool5000.jar [options here]

java -jar TimingIntrusionTool5000.jar --mode=testhash --hash=sha1 --host=localhost --port=60000 --requestTemplate=socket_request.txt --confidenceRequired=0.2 --knownPassword=mcartney


--mode

* testplaintext - Tests the server with a known password, assuming the server stores passwords in plaintext.
* testhash - Tests the server with a known password, assuming the server stored the password as a hash. Use --hash to set which hashing algorithm.
* plaintextlength -  Tries to work out how long a user's plaintext password is. Ensure you use a padded request template to make sure all requests are the same length. This seems unreliable.
* hash - Full on hash based timing attack. Limited to hex encoded basic hashes without salts. This requires wordlist input and output files for wordlist reduction once the hash prefix is guessed.

--hash 

* The hash function the server is using, anything supported by Java will work (MD5, SHA-1, etc). 

--requestTemplateFile

* This is the template where the password candidate is inserted before sending to the server. See the examples supplied. Some text editors (gedit) insert invisible line breaks at the end of a file, this can mess up the attack. The tool will warn of this.

--knownPassword 

* Required for test modes.

--host and --port

* Kinda obvious.

--confidenceRequired

* This signifies what confidence is required before a winner is classified. The default is 0.8, however this can be lowered on servers with low jitter to speed up the attack. This should be increased in high jitter environments.

--charsToGuess

* How many hash characters to guess, defaults to 1. This is enough to reduce the password list by 16 times. Guessing more than one character can be difficult, and more than two (256 times reduction) is probably not worth it.

--threads

* How many request threads. Defaults to 1. You probably shouldn't touch this as multithreaded attacks introduce a heap of jitter on some servers (but not all).

