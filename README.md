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
$ git https://github.com/david618/Simulator
$ cd Simulator
$ mvn install 
</pre>

After Build; the target folder will contain:
- lib folder: all of the jar depdencies
- Simulator.jar: small executable jar without dependencies.
- Simulator-jar-with-dependencies.jar: large executable jar with dependencies.

## Usage

### com.esri.simulator.Tcp

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Tcp 

Usage: Tcp &lt;server&gt; &lt;port&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt;
- Sends lines from file to the specified server and port.  
- The simulator tries to send numrecords at rate requested.
- During the run the simulator counts the records and actual rate it was able to send and outputs that to the screen.

### com.esri.simulator.Kafka

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Kafka 

Usage: Kafka &lt;broker-list-or-hub-name&gt; &lt;topic&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;burst-delay-ms&gt;)
- Sends lines from file to the specified broker-list-or-hub-name topic.  
- The simulator tries to send numrecords at rate requested. 
- If burst-delay-ms is specified the records are send in bursts ever burst-delay-ms milliseconds to achieve the desired rate. For example, if you request 10,000 e/s with a burst delay of 100; the simulator will send at max rate possible 1,000 events every 100 ms.  If not specified the results are sent one every 1/10,000 of a second. 


### com.esri.simulator.Elasticsearch

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.Elasticsearch 
Usage: Elasticsearch &lt;elastic-search-transports&gt; &lt;cluster-name&gt; &lt;index&gt; &lt;type&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;elastic-bulk-num&gt;)

Used to test sending data directly to Elasticsearch from a file.

### com.esri.simulator.FeatureLayerMon

$ java -cp Simulator-jar-with-dependencies.jar com.esri.simulator.FeatureLayerMon 
Usage: FeatureLayerMon &lt;Feature-Layer&gt; (&lt;Seconds-Between-Samples&gt; Default 5 seconds)  

Example:

$ java -cp Simulator.jar com.esri.simulator.FeatureLayerMon http://dj52web.westus.cloudapp.azure.com/arcgis/rest/services/Hosted/FAA-Stream/FeatureServer/0

- The code counts the number of features from the Feature-Layer
- If no count change is detected it will wait
- Each time change is detected a sample is added and output to the screen
- After count stops increasing; least-square fit is used to calculate the rate of change 
- Results are printed to the screen

### com.esri.simulator.ElasticIndexMon
Monitors a Elasticsearch Index count and measures and reports rate of change in count.

$ java -cp target/Simulator.jar com.esri.simulator.ElasticIndexMon
Usage: ElasticIndexMon &lt;ElasticsearchServerPort&gt; &lt;Index/Type&gt; (&lt;username&gt; &lt;password> &lt;sampleRateMS&gt;)

Example:

$ java -cp target/Simulator.jar com.esri.simulator.ElasticIndexMon 172.17.2.5:9200 satellites/satellites "" "" 60000

- Elasticsearch running on 172.17.2.5 on default port of 9200
- The index name is satellites and so is the type (satellites/satellites)
- The quotes are because I wanted to enter 60000 ms (as the sample rate)

*** NOTE: For GeoEvent you can get the username/password for the spatiotemportal datastore using datastore tool "listadmins". ***


### com.esri.simulator.KafkaTopicMon
Monitors a Kafka Topic count and measures and reports rate of change in count.

$ java -cp target/Simulator.jar com.esri.simulator.KafkaTopicMon
Usage: KakfaTopicMon &lt;brokers&gt; &lt;topic&gt; (&lt;sampleRateMS&gt;)

$ java -cp target/Simulator.jar com.esri.simulator.KafkaTopicMon 172.17.2.5:9528 satellites-in 60000

- Connects to Kafka on 172.17.2.5 on port 9528 
- Gets counts for the satellites-in topic
- The sample rate is set to 60,000 ms which is 60 seconds


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




