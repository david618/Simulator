#!/bin/bash

java -jar target/Simulator-jar-with-dependencies.jar a1.trinity.dev 5565 simFile_1000_10s.dat 80000 800000 > t1.txt &
java -jar target/Simulator-jar-with-dependencies.jar a1.trinity.dev 5565 simFile_1000_10s.dat 80000 800000 > t2.txt &
java -jar target/Simulator-jar-with-dependencies.jar a2.trinity.dev 5565 simFile_1000_10s.dat 80000 800000 > t3.txt &
java -jar target/Simulator-jar-with-dependencies.jar a2.trinity.dev 5565 simFile_1000_10s.dat 80000 800000 > t4.txt &
