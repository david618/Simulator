
import json
import urllib2
import sys

host="m1:8080"
hubname="hub01"

debug=True

if __name__ == '__main__':


    numargs = len(sys.argv)
    if debug: print("numargs=" + str(numargs))

    if (numargs == 2):
        hubname = sys.argv[1]
    strUrl = "http://" + host + "/v2/apps/" + hubname
    if debug: print(strUrl)
    url = urllib2.urlopen(strUrl)

    data = json.load(url)

    hubip=str(data['app']['tasks'][0]['ipAddresses'][0]['ipAddress'])
    hubport=str(data['app']['tasks'][0]['ports'][1])
    if debug: print("Hub Info")
    if debug: print(hubip)
    if debug: print(hubport)

    strUrl = "http://" + hubip + ":" + hubport + "/v1/connection/"
    if debug: print(strUrl)
    url = urllib2.urlopen(strUrl)
    data2 = json.load(url)
    if debug: print(data2)

    broker=str(data2['address'][0])

    print("Broker: " + broker)
