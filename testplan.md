## Create Virtual Machines on AWS Instances

You'll need one machine as a Test Server and at least one for ArcGIS.

Test server does not have huge cpu or ram requirements.  Two cores and 8GB RAM is sufficient (AWS m4.large or Azure DS11_V2)

I would recommend at least 4 cores and 16GB RAM for ArcGIS Services (AWS m4.xlarge or Azure DS12_V2)

Pick a supported OS (e.g. CentOS 7.2 or RHEL 7.2)

You'll need to configure security to allow access to the servers.  Making port 6443 and 7443 on ags server accessible will make installation much easier.  You can limit access to your IP and access can be removed after installation is complete.  You should also open 80 and 443 to the ags server.

Web server needs a public DNS entry.  AWS creates one by default; you'll have to configure one for Azure.

Servers should have static private IP's.  AWS uses static private IP's; however, Azure defaults to dynamic private IP's.

Allow all ports between the test server and ags server(s).

## Install and Configure ArcGIS 
There are my [installation notes on github](https://github.com/david618/agsservicetesting/blob/master/installArcGISStack.md)

You should be able to connect to GeoEvent Manager, Portal, and ArcGIS Server as administrator. You should be able to create BDS Data Stores.

## Setup Test Server

### Install some packages
<pre>
# yum -y install epel-release
# yum -y install git
# yum -y install java-1.8.0-openjdk
# yum -y install maven
</pre>

### Clone Simulator
<pre>
$ git clone https://github.com/david618/Simulator
</pre>

Build Simulator
<pre>
$ cd Simulator
$ mvn install
</pre>
Should end with "BUILD SUCCESS"

### Install JMeter (optional)
To support Map/Feature Service Testing
<pre>
$ cd ~
$ curl -O http://apache.org/dist/jmeter/binaries/apache-jmeter-3.1.tgz
</pre>

Verify signature
<pre>
$ md5sum apache-jmeter-3.1.tgz
f439864f8f14e38228fee5fab8d912b0  apache-jmeter-3.1.tgz
</pre>

Untar
<pre>
$ tar xvzf apache-jmeter-3.1.tgz
</pre>

<pre>
$ git clone https://github.com/david618/agsservicetesting
</pre>

## Create GeoEvent Input

Create Tcp Input
- Name: tcp-text-in-faa-stream

Defaults for other options will work with faa-stream sample data.

Here is a few samples from faa-stream.csv file.

<pre>
FAA-Stream,13511116,DLH427,06/22/2013 12:02:00 AM,-55.1166666666667,45.1166666666667,82.660223052638,540,350,A343,DLH,KPHL,EDDF,JET,COMMERCIAL,ACTIVE,GA,"-55.1166666666667,45.1166666666667,350.0"
FAA-Stream,53162756,UAL1653,06/22/2013 12:01:43 AM,-100.216666666667,32.1,139.746823262044,489,390,B738,UAL,KSFO,KAUS,JET,COMMERCIAL,ACTIVE,GA,"-100.216666666667,32.1,390.0"
FAA-Stream,19522513,CTL9,06/22/2013 12:02:20 AM,-96.2333333333333,31.25,344.086017363837,177,86,AC50,CTL9,SGR,DAL,PISTON,COMMERCIAL,ACTIVE,GA,"-96.2333333333333,31.25,86.0"
FAA-Stream,42728207,N362AM,06/22/2013 12:02:02 AM,-121.533333333333,38.25,141.866431910358,114,36,N/A,N,N/A,N/A,N/A,N/A,N/A,N/A,"-121.533333333333,38.25,36.0"
</pre>

Run one sample through to generate the GeoDefinintion.

<pre>
$ cd Simulator
$ java -cp target/Simulator.jar com.esri.simulator.Tcp
Usage: Tcp &lt;server&gt; &lt;port&gt; &lt;file&gt; &lt;rate&gt; &lt;numrecords&gt; (&lt;append-time-csv&gt;)
</pre>

Running the command wihout any arguments returns instructions.

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 1 1 
1,0.9990009990009991
</pre>

The command sends 1 line of the faa-stream.csv file to the service at a rate of 1/s.  The script prints the number of records send and the measured rate. The actual rate will depend on the network speed and responses from the server; you'll notice at higher rates the measured rate will drop off.

In GeoEvent you should see the new GeoDefinition "FAA-Stream".  You can adjust the field names and types as needed; but the defaults will work.

On the Advanced Settings of the service change the "Create Unrecognized Event Definitions" to No. During high velocity tests GeoEvent can fall behind and if this is set to Yes may cause GeoEvent to create spuroius GeoDefinitions. 

## Create GeoEvent Outputs

### Tcp to External Server
- Name: tcp-text-out-faa-stream
- Host: Use Private IP of the test machine 
The defaults for the remaining parameters will work.

NOTE: The service will have a Status Error. This is because we haven't started the TcpSink yet.  It's ok.  You can stop the service for now.

### Add a Feature to an ArcGIS Spatiotemporal Big Data Store
- Name: bds-out-faa-stream-add

Click Create Data Source
- FAA-Stream-Add
Defaults work for rest.


## Open Access to Everyone 
From Portal 

Click on My Content 

Check the FAA-Stream-Add and FAA-Stream-Update Feature and Map Image Layers.

Click Share

Check "Everyone"

This allows the Sinks to access the data without authentication. 

## Create GeoEvent Service

### faa-stream-tcp-tcp
Add tcp-text-in-faa-stream and tcp-text-out-faa-stream and connect input to output.

### faa-stream-tcp-bds-add
Add tcp-text-in-faa-stream and bds-out-faa-stream-add and connect input to output.

Stop all the inputs, outputs, and services.

## Run Sink

### TcpSink

You'll need to terminals on the test server.

Terminal one start TcpSink
<pre>
$ cd ~/Simulator
$ java -cp target/Simulator.jar com.esri.simulator.TcpSink
Usage: TcpSink &lt;port-to-listen-on&gt;
</pre>
As before without any arguments the command returns Usage line.  Run listening on port 5575 as we configured on the GeoEvent Output.

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.TcpSink 5575
</pre>

The command starts and TcpSink is listening for messages. 

In GeoEvent you can start the "tcp-text-out-faa-stream".  It should be green; indicating that connection was successful.

From GeoEvent Turn on the Input (tcp-text-in-faa-stream) and the Service (faa-stream-tcp-tcp).  

From Second terminal on test server.

<pre>
$ cd ~/Simulator
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 100 1000 
1000,99.68102073365232
</pre>

The output indicates that 1000 events were sent and the measure rate was 99.68102073365232/s

Look back at the TcpSink Terminal. You should see the count and rate measured by the TcpSink.

<pre>
1000,100
</pre>
The rate is measured by starting a timer when first event arrives. When messages stop arriving for a few seconds the TCPSink will calculate the rate and reset and wait. 

You can now run additional tests from the Simulator terminal.
<pre>
$ cd ~/Simulator
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 1000 10000
10000,986.6798223976319
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 5000 50000
50000,4708.984742889433
</pre>

The resulting throughput is shown on TcpSink

<pre>
10000 , 985
50000 , 3177
</pre>

You should get the same count on the output.  The rate reflects the output or throughput of the service.  Note that at input Rate of 5,000 the throughput was 3,177.  







FeatureLayerSink

## Run Simulation
Tcp

