# Setup Test Server

For best results you should use a test server that is in the same subnet but is not running the services/apps you are testing.

## Install Support Tools

** Linux **
<pre>
# yum -y install epel-release
# yum -y install git
# yum -y install java-1.8.0-openjdk
# yum -y install maven
</pre>

** Windows **
- Install [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- Install [Git](https://git-scm.com/download/win) 
  -- You can use defaults during install
- Install [Maven](https://maven.apache.org/download.cgi)
  -- Unzip the Binary zip archive and unzip
  -- Update your Path (e.g.  D:\apache-maven-3.3.9\bin)

## Clone Simulator
<pre>
$ git clone https://github.com/david618/Simulator
</pre>

## Build Simulator
<pre>
$ cd Simulator
$ mvn install
</pre>
Should end with "BUILD SUCCESS"

## Install JMeter (optional)
To support Map/Feature Service Testing
<pre>
$ cd ~
$ curl -O http://apache.org/dist/jmeter/binaries/apache-jmeter-3.1.tgz
</pre>

** Windows **
Download [JMeter zip](http://jmeter.apache.org/download_jmeter.cgi)

Verify signature (Linux)
<pre>
$ md5sum apache-jmeter-3.1.tgz
f439864f8f14e38228fee5fab8d912b0  apache-jmeter-3.1.tgz
</pre>

Untar or Unzip
<pre>
$ tar xvzf apache-jmeter-3.1.tgz
</pre>

<pre>
$ git clone https://github.com/david618/agsservicetesting
</pre>

## Trinity

When configuring DCOS cluster you can an additional public agent(s) to support testing.  You can request the additional nodes using the ARM template (Azure) or using a Cloudconfiguration File with additional nodes.  

After the cluster is built take the test agents out of the cluster. 

For Example:

<pre>
$ ssh -i azureuser p2
$ sudo su -
# systemctl stop dcos-mesos-slave-public.service
# systemctl disable dcos-mesos-slave-public.service
</pre>

You can verify that the node disappears from DCOS Dashboard. DCOS will show the node as Unhealthy for a while; then it will disappear.  Usually takes a few minutes.

I used public agents, because they are a little smaller that the private agents and do not have the extra disk space (which is not required on the test server).  You can do the same thing extra private agents or masters.  

You must leave at least 1 public agent (Marathon-LB / Trinity) needs one.

From the test server (e.g. p2) you can now use Mesos-DNS entries.

<pre>

[p2]$ ping hub01.mesos
[p2]$ ping broker-0.hub01.mesos
[p2]$ ping broker-1.hub01.mesos

[p2]$ ping sat01.mesos
[p2]$ ping sat01-node.sat01.mesos
[p2]$ host sat01-node.sat01.mesos
sat01-node.sat01.mesos has address 172.17.2.4
sat01-node.sat01.mesos has address 172.17.2.6

These addresses can be used in the test tools like Simulator; instead of using IP's.
</pre>
