if __name__ == '__main__':
	print("OK")
	#fin = open("simFile_1000_10s.dat")
	fin = open("data0001")
        cnt = 0
	#total_bytes = 0
	running_avg = 0.0
	for line in fin:
		cnt += 1
		#total_bytes += len(line)
		running_avg = running_avg + (len(line) - running_avg)/cnt
	fin.close()
	#print(str(cnt) + "," + str(1.0*total_bytes/cnt) + "," + str(running_avg))
	print(str(cnt) + "," + str(running_avg))
