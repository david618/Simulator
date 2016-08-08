import json

if __name__ == '__main__':
    print "Start"

    fin = open("simFile_5000_10s.dat")
    fout = open("simFile_5000_10s.json", "w")

    for line in fin:
        
        #print line
        field = line.strip().split(",")

        row = {}

        row['tm'] = long(field[0])
        row['id'] = int(field[1])
        row['dtg'] = field[2]
        row['rt'] = field[3]
        row['lon'] = float(field[4])
        row['lat'] = float(field[5])
        row['spd'] = float(field[6])
        row['brg'] = float(field[7])

                
        jsonStr = json.dumps(row)

        #print jsonStr
        fout.write(jsonStr + "\n")

    fin.close()
    fout.close()


        
