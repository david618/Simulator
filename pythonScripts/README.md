# Collection of Python Scripts

## csv2json.py
Converts the simFile data from csv format to json. 

## get_counts.py
Takes two parameters the source app and the sink app name.  The script uses Marathon rest calls to get the ip's and port's then calls the /count for each source/sink to gather count/rates/latency information.

The final line output the number of events read by the source(s), rate at which the source(s) processed the events, the number of events read by the sink(s), the rate at which the sink(s) processed the events.

The rate is a weighted average of any results returned by the count. For example if you do two runs one with 1,000 events (rate1) and the other with 1,000,000 events (rate2).  

Weighted Average =  1000/1001000 * rate1 + 1000000/1001000 * rate2

The services must be configured to use the system provided ports.  

For service command use $PORT0

<pre>
java -cp $MESOS_SANDBOX/rt-jar-with-dependencies.jar org.jennings.rt.sink.kafka.KafkaCnt kafka simFile group1 $PORT0
</pre>

The health check can also be configured to use this port.  Use portIndex instead of port.

<pre>
      "portIndex": 0            
</pre>


## reset_counts.py
Takes two parameters the source app and the sink app name.  The script uses Marathon rest calls to get the ip's and port's then calls the /reset for each source/sink to clear data from previous runs.

## License

http://www.apache.org/licenses/LICENSE-2.0 




