#!/bin/bash

for workers in 1 2 5 10 20
do
    for batch_size in 32 64 128 256 512 1024
    do
        make workers=${workers} batch_size=${batch_size}
    done
done