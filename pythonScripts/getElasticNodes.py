import json
import urllib2
import sys

host="m1:8080"
satname="sat01"

debug=True

if __name__ == '__main__':


    numargs = len(sys.argv)
    if debug: print("numargs=" + str(numargs))

    if (numargs == 2):
        satname = sys.argv[1]

    strUrl = "http://" + host + "/v2/apps/sattasks/" + satname + "/apps/sat" 
    if debug: print(strUrl)
    url = urllib2.urlopen(strUrl)
    data3 = json.load(url)                   
    if debug: print(data3)

    satip=str(data3['app']['tasks'][0]['ipAddresses'][0]['ipAddress'])
    satport=str(data3['app']['tasks'][0]['ports'][0])

    strUrl = "http://" + satip + ":" + satport + "/v1/tasks/"
    if debug: print(strUrl)
    url = urllib2.urlopen(strUrl)
    data4 = json.load(url)
    if debug: print(data4)

    elasticsearch=str(data4[0]['http_address'])

    print("Elasticsearch: " + elasticsearch)
