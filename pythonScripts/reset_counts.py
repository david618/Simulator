#!/usr/bin/python2

# requires install python-httplib2
# apt-get install python-httplib2

# Run using python2
# python2 get_results.py
# The sha-bang is set for ubuntu 16.04

import httplib2

import json

hosts = []
#hosts.append("10.32.0.4")
#hosts.append("10.32.0.5")
#hosts.append("10.32.0.8")
#hosts.append("10.32.0.9")
#hosts.append("10.32.0.10")
#hosts.append("10.32.0.11")
#hosts.append("10.32.0.12")
#hosts.append("10.32.0.14")
#hosts.append("10.32.0.15")
#hosts.append("10.32.0.16")
hosts.append("192.168.56.81")
hosts.append("192.168.56.82")
hosts.append("192.168.56.83")
hosts.append("localhost")

source_port = "14001"
sink_port = "14002"

source_hosts = []
sink_hosts = []

for host in hosts:
	source_hosts.append(host + ":" + source_port)
	sink_hosts.append(host + ":" + sink_port)

#source_hosts = ["192.168.56.81:9000","192.168.56.82:9000","192.168.56.83:9000"]

#sink_hosts = ["192.168.56.81:9001","192.168.56.82:9001","192.168.56.83:9001"]

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