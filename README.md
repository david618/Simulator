Reads simulation file into memory then sends line by line to the specified host and port via TCP.

Example Command line.
$ java -jar Simulator-jar-with-dependencies.jar d1.trinity.dev 5565 ../simFile_1000_10s.dat 100000 1000000

Arguments are:  hostname port file rate numberToSend

This command will send lines from simFile_1000_10s.dat to d1.trinity.dev on port 5565.  It will send 1,000,000 at rate of 100,000 e/s.  If the file has less than 1,000,000 rows it will start over from the top.


