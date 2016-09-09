#!/bin/bash

echo -n "4: ";ssh -i azureuser2 10.32.0.4 "df -h" | grep mesos
echo -n "5: ";ssh -i azureuser2 10.32.0.5 "df -h" | grep mesos
echo -n "6: ";ssh -i azureuser2 10.32.0.6 "df -h" | grep mesos
echo -n "7: ";ssh -i azureuser2 10.32.0.7 "df -h" | grep mesos
echo -n "8: ";ssh -i azureuser2 10.32.0.8 "df -h" | grep mesos
echo -n "9: ";ssh -i azureuser2 10.32.0.9 "df -h" | grep mesos
echo -n "11: ";ssh -i azureuser2 10.32.0.11 "df -h" | grep mesos
echo -n "12: ";ssh -i azureuser2 10.32.0.12 "df -h" | grep mesos
echo -n "14: ";ssh -i azureuser2 10.32.0.14 "df -h" | grep mesos
echo -n "16: ";ssh -i azureuser2 10.32.0.16 "df -h" | grep mesos
echo -n "18: ";ssh -i azureuser2 10.32.0.18 "df -h" | grep mesos
echo -n "19: ";ssh -i azureuser2 10.32.0.19 "df -h" | grep mesos
echo -n "20: ";ssh -i azureuser2 10.32.0.20 "df -h" | grep mesos
echo -n "21: ";ssh -i azureuser2 10.32.0.21 "df -h" | grep mesos
echo -n "22: ";ssh -i azureuser2 10.32.0.22 "df -h" | grep mesos
