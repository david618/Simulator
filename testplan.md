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


### Install JMeter (optional)
To support Map/Feature Service Testing
<pre>
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

### Clone agsservicetesting
<pre>
$ git clone https://github.com/david618/agsservicetesting
</pre>

## Create GeoEvent Input
Tcp Input

Run one sample through to generate the GeoDefinintion.

Adjust service and GeoDefinition as needed.

## Create GeoEvent Output
Tcp to External Server

Update BDS

Add BDS

## Create Data Store

From Portal allows "Everyone" Access to the Feature Service.
Create GeoEvent Service



## Run Sink
TcpSink

FeatureLayerSink



## Run Simulation
Tcp

