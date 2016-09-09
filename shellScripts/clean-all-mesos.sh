#!/bin/bash

ssh -i azureuser2 10.32.0.4 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.4 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.4 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.4 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.4 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.5 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.5 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.5 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.5 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.5 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.6 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.6 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.6 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.6 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.6 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.7 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.7 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.7 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.7 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.7 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.8 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.8 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.8 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.8 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.8 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.9 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.9 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.9 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.9 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.9 "df -h" | grep mesos

#ssh -i azureuser2 10.32.0.10 "df -h" | grep mesos
#ssh -i azureuser2 10.32.0.10 "sudo systemctl stop dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.10 "sudo rm -rf /var/lib/mesos/slave/*"
#ssh -i azureuser2 10.32.0.10 "sudo systemctl start dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.10 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.11 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.11 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.11 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.11 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.11 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.12 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.12 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.12 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.12 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.12 "df -h" | grep mesos

#ssh -i azureuser2 10.32.0.13 "df -h" | grep mesos
#ssh -i azureuser2 10.32.0.13 "sudo systemctl stop dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.13 "sudo rm -rf /var/lib/mesos/slave/*"
#ssh -i azureuser2 10.32.0.13 "sudo systemctl start dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.13 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.14 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.14 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.14 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.14 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.14 "df -h" | grep mesos

#ssh -i azureuser2 10.32.0.15 "df -h" | grep mesos
#ssh -i azureuser2 10.32.0.15 "sudo systemctl stop dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.15 "sudo rm -rf /var/lib/mesos/slave/*"
#ssh -i azureuser2 10.32.0.15 "sudo systemctl start dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.15 "df -h" | grep mesos
#
ssh -i azureuser2 10.32.0.16 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.16 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.16 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.16 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.16 "df -h" | grep mesos

#ssh -i azureuser2 10.32.0.17 "df -h" | grep mesos
#ssh -i azureuser2 10.32.0.17 "sudo systemctl stop dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.17 "sudo rm -rf /var/lib/mesos/slave/*"
#ssh -i azureuser2 10.32.0.17 "sudo systemctl start dcos-mesos-slave"
#ssh -i azureuser2 10.32.0.17 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.18 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.18 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.18 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.18 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.18 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.19 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.19 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.19 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.19 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.19 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.20 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.20 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.20 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.20 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.20 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.21 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.21 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.21 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.21 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.21 "df -h" | grep mesos

ssh -i azureuser2 10.32.0.22 "df -h" | grep mesos
ssh -i azureuser2 10.32.0.22 "sudo systemctl stop dcos-mesos-slave"
ssh -i azureuser2 10.32.0.22 "sudo rm -rf /var/lib/mesos/slave/*"
ssh -i azureuser2 10.32.0.22 "sudo systemctl start dcos-mesos-slave"
ssh -i azureuser2 10.32.0.22 "df -h" | grep mesos

