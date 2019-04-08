#!/bin/bash

for workers in 10
do
    for batch_size in 64 128 256 512 1024
    do
        make workers=${workers} batch_size=${batch_size}
    done
done