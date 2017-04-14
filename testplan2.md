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

Here are insructions for [Setup Test Server](SetupTestServer.md)


## Create GeoEvent Input

Create Poll an External Website for JSON
- Name: url-poll-in-satellites
- URL: http://esri105.westus.cloudapp.azure.com/websats/satellites
- Create GeoDef: Yes
- GeoEvent Def: satellites
- Frequency: 600 
- Construct Geom: Yes
- X Geom Field: lon
- Y Geom Field: lat
- Parameters: f=json <<  This will grab ~700 events per call


<pre>
[{"dtg":"5-APR-2017 18:54:16.16","num":"36585","name":"GPS BIIF-1  (PRN 25)","alt":20321.476842223765,"lon":48.520987822868875,"lat":-4.219916413962587,"timestamp":1491418456157},{"dtg":"5-APR-2017 18:54:16.16","num":"41019","name":"GPS BIIF-11  (PRN 10)","alt":20220.400717834695,"lon":16.346283264743136,"lat":-34.63953696029579,"timestamp":1491418456157},{"dtg":"5-APR-2017 18:54:16.16","num":"40730","name":"GPS BIIF-10  (PRN 08)","alt":20136.489846528286,"lon":-78.47003310960173,"lat":-4.924285293021734,"timestamp":1491418456157},{"dtg":"5-APR-2017 18:54:16.16","num":"41328","name":"GPS BIIF-12  (PRN 32)","alt":20176.535360033748,"lon":-11.456218951461679,"lat":-51.798077482405354,"timestamp":1491418456157}]
</pre>

Start the input.  This will create the GeoDefintion.

Stop the Input.

## Create GeoEvent Output

- Name: bds-out-satellites

Click Create Data Source
- satellites
Defaults work for rest.


## Open Access to Map and Feature Services
From Portal 

Click on My Content 

Check the satellites Feature and Map Image Layers.

Click Share

Check "Everyone"

This allows the Sinks to access the data without authentication. 

## Create GeoEvent Service

### satellites
Add url-poll-in-satellites and bds-out-satellites.  Connect input to output.

## Pre Test Check
Modify the input aand change Create GeoDef to No and set GeoDef name (satellites).

Start Input, Output, and Service and ensure the data flows.

Stop the Input.

## Run Tests

### Run FeatureLayerMon 

On the Test Server
<pre>
cd ~/Simulator
java -cp target/Simulator.jar com.esri.simulator.FeatureLayerMon https://dj32ags.westus.cloudapp.azure.com/arcgis/rest/services/Hosted/satellites/FeatureServer/0
</pre>

### Run ElasticIndexMon

Log into the AGS server as arcgis user. 

<pre>
cd /home/arcgis/arcgis/datastore/tools
./listadminusers.sh
Admin user for spatiotemporal big data store bds_767r1fec
================================================
Store admin user....... els_xal5a3z / 767r1fec88
</pre>

Now run Elasticsearch Monitor on Test Server in another SSH session.

<pre>
cd ~/Simulator
java -cp target/Simulator.jar com.esri.simulator.ElasticIndexMon dj32ags:9220 satellites/satellites els_xal5a3z  767r1fec88
</pre>

On the Input change the frequency to 1 second.  This will result in about 700 events/s.  As it's running you should see counts on both FeatureLayerMon and ElasticIndexMon.  Let the input run so that you collect a least 3 samples on each. Stop the input.

Note the Rates.  My first test was measured 692 on both.

To get faster rates change the Parmeters to "f=json&n=2";  this will return 2 samples per satellite or around 1,400/s.  

Rates measured with 1,400/s in for this test were 

<pre>
1,1491419869051,59001
2,1491419874066,65894
3,1491419879094,72904
4,1491419884114,79914
5,1491419889162,86945
6,1491419894202,93393
7,1491419899219,95336
Removing: 1491419899219,95336
36452 , 1375.19, 0.0090
</pre>

Repeat for higher values of n. 

As some point the output will reach a max rate.  If you drive the input faster that the max output eventually you will lose events.


## Summary
These measurements can help users decide on architecture changes to meet there needs.

