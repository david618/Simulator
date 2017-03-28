# Simulator

Reads simulation file into memory then sends line by line to the specified host and port via TCP or directly to Kafka/Elasticsearch.

## Installation

Originally created in NetBeans 8.1; recent work in IntelliJ.

Tested with maven 3.3.9
$ mvn install 

The target folder will contain:
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



### Notes

Details on how the simFile data (simFile*) in this project are covered in [Create Flight Simulation Data Blog Post](http://davidssysadminnotes.blogspot.com/2016/07/create-flight-simulation-data.html).

Several Python scripts were created to support gathering test results in DCOS.  Details on how these scripts were used are in the [Performance Testing Kafka on DCOS Blog Post](http://davidssysadminnotes.blogspot.com/2016/08/performance-testing-kafka-on-dcos.html)

## License

http://www.apache.org/licenses/LICENSE-2.0 




