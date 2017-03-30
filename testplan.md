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


## Open Access to Map and Feature Services
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

## Run Tests

### Tcp-NoOp-Tcp

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

Stop Output.

Used Ctrl-C to stop TcpSink.

### Tcp-NoOp-AddBDS

NOTE: During testing you should only one run inputs, service, outputs that are being tested.

Start FeatureLayerSink

<pre>
$ cd ~/Simulator
$ java -cp target/Simulator.jar com.esri.simulator.FeatureLayerSink
Usage: FeatureLayerSink &lt;Feature-Layer&gt; (&lt;Seconds-Between-Samples&gt; Default 5 seconds) 
</pre>

This tool call the feature service and returns the count every few seconds (default is 5). Self signed certs are OK; however, the cert must match the host name. I added dj32web.westus.cloudapp.azure.com to hosts file on the test server associating it's this name to the private IP. NOTE: I might be able to change the code to ignore the cert; but for now you have to add an entry to hosts and use 6443.

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.FeatureLayerSink https://dj32ags.westus.cloudapp.azure.com:6443/arcgis/rest/services/Hosted/FAA-Stream-Add/FeatureServer/0
</pre>

The sink starts and checks the count every 5 seconds. If it sees count changing it will start collecting samples.

On Simulator terminal start a Simulation.
<pre>
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 1000 10000
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 1000 100000
</pre>

On the Sink Terminal you'll see output.

<pre>
1,1490885417792,1190
2,1490885422821,10000
Removing: 1490885422821,10000
Not enough samples to calculate rate. 
1,1490885462974,14534
2,1490885468014,18575
3,1490885473036,24208
4,1490885478058,28951
5,1490885483075,34244
6,1490885488089,39216
7,1490885493112,44057
8,1490885498184,49169
9,1490885503214,53955
10,1490885508230,58937
11,1490885513247,63970
12,1490885518271,69052
13,1490885523305,73865
14,1490885528322,78912
15,1490885533340,83957
16,1490885538373,88913
17,1490885543529,93892
18,1490885548564,98766
19,1490885553639,103904
20,1490885558681,109001
21,1490885563704,110000
Removing: 1490885563704,110000
100000 , 990.14, 0.0015
</pre>

Notic on my first run I didn't send enough data.  The Sink does a count every 5 seconds. I've set the Sink to require a minimum of 5 samples. After count stop increasing the sink uses the samples to calculate the rate using Least Squares.  The throughput rate in this example was 990.14/s with a standard error of 0.0015.  Lower standard error implies a good fit to the data.

The samples include the first point when count as changed. The last point is excluded when no change is detected.

<pre>
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 5000 100000
100000,4821.600771456124
$ java -cp target/Simulator.jar com.esri.simulator.Tcp dj32ags 5565 faa-stream.csv 5000 200000
200000,4809.542131589073
</pre>

Resulting Sink Output
<pre>
1,1490885809630,110656
2,1490885814658,117668
3,1490885819689,126145
4,1490885824726,133048
5,1490885829754,141602
6,1490885834797,154287
7,1490885839826,169343
8,1490885844877,182536
9,1490885849898,194972
10,1490885854917,209507
11,1490885859946,210000
Removing: 1490885859946,210000
100000 , 2208.57
1,1490885930225,212197
2,1490885935264,219967
3,1490885940307,226822
4,1490885945364,235694
5,1490885950525,243671
6,1490885955586,250106
7,1490885960629,258658
8,1490885965678,263910
9,1490885970716,272001
10,1490885975744,286001
11,1490885980822,299337
12,1490885985883,312985
13,1490885990952,327556
14,1490885996006,338110
15,1490886001075,353667
16,1490886006118,367553
17,1490886011150,381927
18,1490886016304,394289
19,1490886021330,409528
20,1490886026363,410000
Removing: 1490886026363,410000
200000 , 2177.58, 0.0737
</pre>

NOTE: The standard Error is not output unless there are more than 10 samples.

As before if you drive at higher rates you'll notice the output is slower than the input.
