# Simulator

Reads simulation file into memory then sends line by line to the specified host and port via TCP or directly to Kafka/Elasticsearch.

## Installation

### Pre Reqs
You must have java, maven, and git installed. 

For example in CentOS:
<pre>
# sudo yum install epel-release 
# sudo yum install git maven
</pre>

First line installs the "Extra Packages for Enterprise Linux" repository to yum.

Second line install git and maven.  This install will install java also if it's not installed.

### Build Simulator

<pre>
$ git clone https://github.com/david618/Simulator
$ cd Simulator
$ mvn install 
</pre>

After Build; the target folder will contain:
- lib folder: all of the jar depdencies
- Simulator.jar: small executable jar without dependencies.
- Simulator-jar-with-dependencies.jar: large executable jar with dependencies.

## Usage

### com.esri.simulator.Elasticsearch

<pre>
$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Elasticsearch 
Usage: Elasticsearch &lt;elastic-search-transports&gt; &lt;cluster-name&gt; &lt;index&gt; &lt;type&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;elastic-bulk-num&gt;)
</pre>

Used to test sending data directly to Elasticsearch from a file.

### com.esri.simulator.ElasticIndexMon
Monitors a Elasticsearch Index count and measures and reports rate of change in count.

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.ElasticIndexMon
Usage: ElasticIndexMon &lt;ElasticsearchServerPort&gt; &lt;Index/Type&gt; (&lt;username&gt; &lt;password> &lt;sampleRate&gt;)
</pre>

Example:

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.ElasticIndexMon 172.17.2.5:9200 satellites/satellites "" "" 60

- Elasticsearch running on 172.17.2.5 on default port of 9200
- The index name is satellites and so is the type (satellites/satellites)
- The quotes are because I wanted to enter 60 s (as the sample rate)
</pre>

**NOTE:** For GeoEvent you can get the username/password for the spatiotemportal datastore using datastore tool "listadmins". 

### com.esri.simulator.FeatureLayerMon

<pre>
$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.FeatureLayerMon 
Usage: FeatureLayerMon &lt;Feature-Layer&gt; (&lt;Seconds-Between-Samples&gt; Default 5 seconds)  
</pre>

Example:

<pre>
$ java -cp Simulator.jar com.esri.simulator.FeatureLayerMon http://dj52web.westus.cloudapp.azure.com/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0
</pre>

- The code counts the number of features from the Feature-Layer
- If no count change is detected it will wait
- Each time change is detected a sample is added and output to the screen
- After count stops increasing; least-square fit is used to calculate the rate of change 
- Results are printed to the screen

### com.esri.simulator.Http

Post lines from a file to a URL.

<pre>
java -cp target/Simulator.jar com.esri.simulator.Http  
Usage: Http &lt;url&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt;
</pre>

Parameters
- url: The url you want to send the ports to
- file: The name of the file to read lines from 
- rate: Desired rate. App will try to dynamically adjust to achieve this rate 
- numrecords: Number of lines to post. Once file is exhausted it will automatically start from top of file again 

Example
<pre>
java -cp target/Simulator.jar com.esri.simulator.Http  http:<i></i>//marathon-lb.marathon.mesos:10004/rtgis/receiver/planes/txt  planes00001.1M 500 5000
</pre>

This command
- Posts lines from file planes00001.1M to http:<i></i>//marathon-lb.marathon.mesos:10004/rtgis/receiver/planes/txt
- Tries to send at 500/s
- Send 5,000 posts; reusing the file as needed.

Example Output

<pre>
500,395
1000,410
1500,418
2000,426
2500,432
3000,437
3500,440
4000,422
4500,426
5000,428
</pre>

The command outputs
- Every rate samples (e.g. 500) output the count and rate 
- Based on the rate the timing is adjusted to try to achieve the requested rate
- The rate reported is the rate actually achieved

### com.esri.simulator.Http2 (devlopment)

Post lines from a file to URL. 

Changes
- Added indication of error (error count) to output
- Added additional optional parameter to support threads 
- Added support to lookup ip and ports and send directly to Marathon App instances 

<pre>
java -cp target/Simulator.jar com.esri.simulator.Http2  
Usage: Http &lt;url&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;numthreads=1&gt;)
</pre>

Parameters
- url: The url you want to send the ports to. Server name can be "app[marathon-app-name]".
  - If app[marathon-app-name] is used Http2 looks up ip:port for each instance
  - Each thread is assigned an ip:port in a round-robin fashion
- file: The name of the file to read lines from (e.g. planes00001.1M)
- rate: Desired rate. App will try to dynamically adjust to achieve this rate (e.g. 50000)
- numrecords: Number of lines to post. Once file is exhausted it will automatically start from top of file again (e.g. 1000000)
- numthreads: Optional parameter defaults to 1.

Example
<pre>
java -cp target/Simulator.jar com.esri.simulator.Http2  http<i></i>://app[sits/rcv-txt-rest-planes]/rtgis/receiver/planes/txt  planes00001.1M 50000 1000000 64
</pre>

This command
- Looks up the ports and ip's for each instance of sits/rcv-txt-rest-planes
- Creates 64 threads; ip and port of instance assigned for each thread (e.g. http:<i></i>//172.17.2.6:3455//rtgis/receiver/planes/txt). The ip:port's are assigned in round-robin fashion.
- The lines from the file planes00001.1M are added to a shared blocked queue at the rate specified
- The threads read lines from the queue and send them to the url they were assigned

Exaple Output

<pre>
172.17.2.9:26264
172.17.2.8:28203
172.17.2.4:11865
172.17.2.6:2152
172.17.2.7:1718
172.17.2.5:12370
15620,0,3042
31925,0,3150
48303,0,3191
...
969367,0,3339
987061,0,3343
1000000,0,3330
Queue Empty
1000000,0,3330
</pre>

The command outputs
- The IP:PORT's found for this Marathon App.
- Current Count Sent, Number of Errors (Should be zero), and rate achieved every 5 seconds.
- The rate send is often less than rate requested; because of back pressure from the endpoint

Number of Errors is the number of responses that were not HTTP 200. This happens if the URL is invalid or the end point is having some problem.



### com.esri.simulator.Kafka

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Kafka 

Usage: Kafka &lt;broker-list-or-hub-name&gt; &lt;topic&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;burst-delay-ms&gt;)
- Sends lines from file to the specified broker-list-or-hub-name topic.  
- The simulator tries to send numrecords at rate requested. 
- If burst-delay-ms is specified the records are send in bursts ever burst-delay-ms milliseconds to achieve the desired rate. For example, if you request 10,000 e/s with a burst delay of 100; the simulator will send at max rate possible 1,000 events every 100 ms.  If not specified the results are sent one every 1/10,000 of a second. 

### com.esri.simulator.KafkaTopicMon
Monitors a Kafka Topic count and measures and reports rate of change in count.

$ java -cp target/Simulator.jar com.esri.simulator.KafkaTopicMon
Usage: KakfaTopicMon &lt;brokers&gt; &lt;topic&gt; (&lt;sampleRate&gt;)

$ java -cp target/Simulator.jar com.esri.simulator.KafkaTopicMon 172.17.2.5:9528 satellites-in 60

- Connects to Kafka on 172.17.2.5 on port 9528 
- Gets counts for the satellites-in topic
- The sample rate is set to 60; which is 60 seconds
- At or around Kafka 0.10.x; when starting the tool displays a bunch of what looks like INFO messages from logger; if you append a redirect for error messages (e.g.  2>stderr.txt) to the command the messages will be hidden. Tried to configure logger for the tool but it did not help with these Error messages.


### com.esri.simulator.Tcp

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Tcp 

Usage: Tcp &lt;server&gt; &lt;port&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt;
- Sends lines from file to the specified server and port.  
- The simulator tries to send numrecords at rate requested.
- During the run the simulator counts the records and actual rate it was able to send and outputs that to the screen.
- There is a limit to the rate Tcp can achieve; which depends on hardware and network speed; Memmory seems to be a big limiter on max speed (on i7 computer with 16G of memory max rate was around 150,000/s)
- There is also a limit on the size of the file. I was able to run with a file containing 5 Million (~100 byte) lines; however, it would not load a 10 Million line file.



### com.esri.simulator.TcpSink

$ java -cp target/Simulator.jar com.esri.simulator.TcpSink
Usage: TcpSink &lt;port-to-listen-on&gt; (&lt;sample-every-N-records/1000&gt;) (&lt;display-messages/false&gt;)

- Listens on the port-to-listen-on for TCP 
- Counts features arriving 
- Adds sample every sample-every-N-records samples; defaults to 1,000
- Five seconds after features stop arriving the rate is calcuated and output to screen; then resets and starts listening again
- Setting display-messages to true will cause the sink to just display messages

### com.esri.simulator.WebSocketSink

$ java -cp target/Simulator.jar com.esri.simulator.WebSocketSink
Usage: WebSocketSink &lt;ws-url&gt; (&lt;timeout-ms&gt;) (&lt;sample-every-N-records/1000&gt;) (&lt;display-messages/false&gt;)

$ java -cp target/Simulator.jar com.esri.simulator.WebSocketSink  ws://localhost:8080/websats/SatStream/subscribe
- Connects to the websocket 
- Uses default timeout-ms; Waits for data; after 10000 ms (10s) disconnect and reconnects
- The default sample rate is every 1,000 records

$ java -cp target/Simulator.jar com.esri.simulator.WebSocketSink  ws://localhost:8080/websats/SatStream/subscribe 100 true
- Connects to the websocket 
- Sample rate is 100 samples (This would be better for measuring slow rates)
- Setting display-messages to true will cause the sink to just display messages

### Notes

Details on how the simFile data (simFile*) in this project are covered in [Create Flight Simulation Data Blog Post](http://davidssysadminnotes.blogspot.com/2016/07/create-flight-simulation-data.html).

Several Python scripts were created to support gathering test results in DCOS.  Details on how these scripts were used are in the [Performance Testing Kafka on DCOS Blog Post](http://davidssysadminnotes.blogspot.com/2016/08/performance-testing-kafka-on-dcos.html)

## License

http://www.apache.org/licenses/LICENSE-2.0 




