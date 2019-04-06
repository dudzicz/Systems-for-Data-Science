#!/bin/bash

if [[ $# -lt 2 ]]
then
    echo "Usage ${0} <workers> <batch_size>"
    exit
fi

WORKERS=${1}
BATCH_SIZE=${2}
kubectl get pods svm 2>&1 > /dev/null
err=$?
if [[ ${err} -eq 0 ]]
then
    kubectl delete pods svm
	until kubectl get pod svm 2>&1 >/dev/null
	do
	    sleep 1
	done
fi
echo "Running distributed SGD with $WORKERS workers and $BATCH_SIZE batch size"
${SPARK_HOME}/bin/spark-submit --class main.Main --properties-file Spark/spark_conf --conf spark.executor.instances=${WORKERS} local:///data/app/SVM.jar ${BATCH_SIZE}
kubectl cp cs449g1/data-pod:/data/log/${WORKERS}_${BATCH_SIZE} logs/${WORKERS}_${BATCH_SIZE}