#!/bin/bash

make
sh run.sh hogwild 1 256
sh run.sh hogwild 2 256
sh run.sh hogwild 5 256
sh run.sh hogwild 10 256
sh run.sh hogwild 15 256
sh run.sh lock 1 256
sh run.sh lock 2 256
sh run.sh lock 5 256
sh run.sh lock 10 256
sh run.sh lock 15 256