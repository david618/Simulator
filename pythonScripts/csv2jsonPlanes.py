import json
import re



if __name__ == '__main__':
    print "Start"


    PATTERN = re.compile(r'''((?:[^,"']|"[^"]*"|'[^']*')+)''')

    fin = open("planes00001")
    fout = open("planes00001.json", "w")

    for line in fin:
        
        #print line
        #field = line.strip().split(",")
	field = PATTERN.split(line.strip())[1::2]
	#print field

        row = {}

        row['id'] = int(field[0])
        row['ts'] = long(field[1])
        row['speed'] = float(field[2])
        row['dist'] = float(field[3])
        row['bearing'] = float(field[4])
        row['rtid'] = int(field[5])
        row['orig'] = field[6].replace('"','')
        row['dest'] = field[7].replace('"','')
        row['secsToDep'] = int(field[8])
        row['lon'] = float(field[9])
        row['lat'] = float(field[10])

                
        jsonStr = json.dumps(row)

        #print jsonStr
        fout.write(jsonStr + "\n")

    fin.close()
    fout.close()


        
