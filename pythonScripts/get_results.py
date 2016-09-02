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

numsources = 0
source_cnt = 0
source_rate = 0.0
source_latency = 0.0

numsinks = 0
sink_cnt = 0
sink_rate = 0.0
sink_latency = 0.0


print("Sources")
for host in source_hosts:
    print(host)

    try:
        
        conn = httplib2.Http(timeout=1)

        resp, resp_body = conn.request("http://" + host + "/count")

        data = json.loads(resp_body)

        i = 0
        cnts = data['counts']
        rates = data['rates']
        latencies = data['latencies']

        num = len(cnts)
        totalcnt = sum(cnts)
        source_cnt += totalcnt

        avgrate = 0
        avglatency = 0.0

        while i < num:

            print(str(i) + ":" + str(cnts[i]) + ":" + str(rates[i]) + ":" + str(latencies[i]))
            avgrate += cnts[i]/float(totalcnt)*rates[i]
            avglatency += cnts[i]/float(totalcnt)*latencies[i]
            i += 1

        source_rate += avgrate
        source_latency += avglatency

        print(data['tm'])

        if totalcnt > 0: numsources += 1

    except Exception as e:
        print(e.message)

if numsources > 1:
    source_latency = source_latency/numsources

print
print("Sinks")
for host in sink_hosts:
    print(host)

    try:
        conn = httplib2.Http()

        resp, resp_body = conn.request("http://" + host + "/count")

        data = json.loads(resp_body)

        i = 0
        cnts = data['counts']
        rates = data['rates']
        latencies = data['latencies']

        num = len(cnts)
        totalcnt = sum(cnts)
        sink_cnt += totalcnt

        avgrate = 0
        avglatency = 0.0

        while i < num:
            print(str(i) + ":" + str(cnts[i]) + ":" + str(rates[i]) + ":" + str(latencies[i]))
            avgrate += cnts[i]/float(totalcnt)*rates[i]
            avglatency += cnts[i]/float(totalcnt)*latencies[i]
            i += 1

        sink_rate += avgrate
        sink_latency += avglatency

        print(data['tm'])

        if totalcnt > 0: numsinks += 1

    except Exception as e:
        print(e.message)

if numsinks > 1:
    sink_latency = sink_latency/numsinks



print
print("Summary")

print("Source Count: " + str(source_cnt))
print("Source Rate: " + str(source_rate))
print("Source Latency: " + str(source_latency))
print("Number of Sources: " + str(numsources))
print 
print("Sink Count: " + str(sink_cnt))
print("Sink Rate: " + str(sink_rate))
print("Sink Latency: " + str(sink_latency))
print("Number of Sinks: " + str(numsinks))

fmt="{:.0f}"
print
print(str(source_cnt) + "," + fmt.format(source_rate) + "," + str(sink_cnt) + "," + fmt.format(sink_rate))
