#!/bin/bash

for workers in 20 10 5 2 1
do
    for batch_size in 32 64 128 256 512 1024
    do
        sh run.sh ${workers} ${batch_size}
    done
done