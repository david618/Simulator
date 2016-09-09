# Collection of Shell Scripts used during testing

## clean-all-mesos.sh 
During testing I ocassionally needed to uninstall and reinstall Kafka and Elasticsearch using DCOS. The application often leave behing data that fills the hard drives (see. df-all-mesos.sh). I wrote a blog post on how to use the script.  http://davidssysadminnotes.blogspot.com/2016/09/clear-dcos-cluster.html

## df-all-mesos.sh
Displays disk usage for all of the nodes in the cluster.

## tesh.sh, test2.sh, test16.sh
These are scripts that run multiple instances of the Simulator.

## rutest15.sh
This is an example of a script that runs test15.sh on several servers at the same time. Its quicker and easier than manually tring to run simulators on multiple servers. You'll need to create a keys to allows ssh without a password (e.g. ssh-keygen and ssh-copy-id).

## License
http://www.apache.org/licenses/LICENSE-2.0 




