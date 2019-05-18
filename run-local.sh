#!/usr/bin/env bash

if [[ $# -ne 3 ]]
then
    echo "Usage ${0} <mode> <workers> <batch_size>"
    exit
fi

scala -J-Xmx16g SVM.jar ${1} ${2} ${3}