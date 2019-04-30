#!/usr/bin/env bash

if [[ $# -lt 2 ]]
then
    echo "Usage ${0} <workers> <batch_size>"
    exit
fi

WORKERS=${1}
BATCH_SIZE=${2}
kubectl get pods svm-${WORKERS}-${BATCH_SIZE} 2>&1 > /dev/null
err=$?
if [[ ${err} -eq 0 ]]
then
    kubectl delete pods svm-${WORKERS}-${BATCH_SIZE}
    kubectl get pod svm-${WORKERS}-${BATCH_SIZE} 2>&1 > /dev/null
	while kubectl get pod svm-${WORKERS}-${BATCH_SIZE} 2>&1 > /dev/null
	do
	    sleep 1
	done
fi
echo "Running distributed SGD with $WORKERS workers and $BATCH_SIZE batch size"