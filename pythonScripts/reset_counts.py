#!/usr/bin/python2

# requires install python-httplib2
# apt-get install python-httplib2

# Run using python2
# python2 get_counts.py
# The sha-bang is set for ubuntu 16.04

# Windows
# pip isntall httplib2 

import httplib2
import sys
import json

source_hosts = []
sink_hosts = []

args = sys.argv

numargs = len(args)

if numargs != 3:
        raise Exception("reset_counts2.py source-name sink-name")
source_name = args[1]
sink_name = args[2]

try:
        # Try to get tasks for source
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://master.mesos:8080/v2/apps/" + source_name)

        data = json.loads(resp_body)        

        tasks = data['app']['tasks']

        for task in tasks:
                source_hosts.append(task['host'] + ":" + str(task['ports'][0]))                
                
except Exception as e:
        print("Failed to connect")


try:
        # Try to get tasks for source
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://master.mesos:8080/v2/apps/" + sink_name)

        data = json.loads(resp_body)        

        tasks = data['app']['tasks']

        for task in tasks:
                sink_hosts.append(task['host'] + ":" + str(task['ports'][0]))                
                
except Exception as e:
        print("Failed to connect")

print("Sources")
for host in source_hosts:
    print(host)

    try:

        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://" + host + "/reset")
        
        data = json.loads(resp_body)

        print(data)
        
    
    except Exception as e:
        #print(e.message)
        print("Failed to connect")
        
print   
print("Sinks")
for host in sink_hosts:
    print(host)

    try:

        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://" + host + "/reset")
        
        data = json.loads(resp_body)

        print(data)
        
    
    except Exception as e:
        #print(e.message)
        print("Failed to connect")
