#!/usr/bin/python2
import httplib2
import sys

# requires install python-httplib2
# apt-get install python-httplib2

# Run using python2
# python2 get_results.py
# The sha-bang is set for ubuntu 16.04

# Windows
# pip isntall httplib2 

import json

source_hosts = []
sink_hosts = []

args = sys.argv

numargs = len(args)

if numargs != 3:
        raise Exception("get_results2.py source-name sink-name")
source_name = args[1]
sink_name = args[2]

try:
        # Try to get tasks for source
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://master.mesos/marathon/v2/apps/" + source_name)

        data = json.loads(resp_body)        

        tasks = data['app']['tasks']

        for task in tasks:
                source_hosts.append(task['host'] + ":" + str(task['ports'][0]))                
                
except Exception as e:
        print("Failed to connect")


try:
        # Try to get tasks for source
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://master.mesos/marathon/v2/apps/" + sink_name)

        data = json.loads(resp_body)        

        tasks = data['app']['tasks']

        for task in tasks:
                sink_hosts.append(task['host'] + ":" + str(task['ports'][0]))                
                
except Exception as e:
        print("Failed to connect")


print source_hosts
print sink_hosts

source_cnt = 0
source_rate = 0.0

sink_cnt = 0
sink_rate = 0.0

print("Sources")
for host in source_hosts:
    print(host)

    try:
        
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://" + host + "/count")

        data = json.loads(resp_body)

        for cnt in data['counts']:
            print(cnt)
            source_cnt += cnt

        for rate in data['rates']:
            print(rate)
            source_rate += rate

        print(data['tm'])
    except Exception as e:
        print("Failed to connect")
        

print
print("Sinks")
for host in sink_hosts:
    print(host)

    try:
        conn = httplib2.Http()

        resp, resp_body = conn.request("http://" + host + "/count")

        data = json.loads(resp_body)
        
        cnt = data['count']
        rate = data['rate']
        print(cnt)
        print(rate)
        sink_cnt += cnt
        sink_rate += rate
        print(data['tm'])
    except Exception as e:
        print("Failed to connect")


print
print("Summary")

print("Source Count: " + str(source_cnt))
print("Source Rate: " + str(source_rate))
print 
print("Sink Count: " + str(sink_cnt))
print("Sink Rate: " + str(sink_rate))

print
print(str(source_cnt) + "," + str(source_rate) + "," + str(sink_cnt) + "," + str(sink_rate))
