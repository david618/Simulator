import json
import csv

if __name__ == '__main__':
    print "Start"

    fin = open("data0001")
    fout = open("data0001.json", "w")

    csvFin = csv.reader(fin)

    n = 0

    for field in csvFin:

        row = {}

        row['id'] = int(field[0])
        row['ts'] = long(field[1])
        row['spd'] = float(field[2])
        row['dst'] = float(field[3])
        row['brn'] = float(field[4])
        row['rid'] = int(field[5])
        row['loc'] = field[6]
        row['s2d'] = int(field[7])
        row['lon'] = float(field[8])
        row['lat'] = float(field[9])
            
        jsonStr = json.dumps(row)
    
        fout.write(jsonStr + "\n")


    fin.close()
    fout.close()
