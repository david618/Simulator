#!/bin/bash

./test15.sh
ssh -i azureuser2 172.16.0.6 './test15.sh'
ssh -i azureuser2 172.16.0.7 './test15.sh'
ssh -i azureuser2 10.0.0.5 './test15.sh'
ssh -i azureuser2 10.0.0.6 './test15.sh'

