#!/bin/bash

for workers in 20 10 5 2 1
do
    for batch_size in 32 64 128 256 512 1024
    do
        make workers=${workers} batch_size=${batch_size}
    done
done