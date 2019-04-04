#!/usr/bin/env bash

if [[ $# -lt 1 ]]
then
    echo "Usage ${0} <workers>"
    exit
fi

WORKERS=${1}
kubectl delete pods svm
echo "Copying SVM.jar..."
kubectl cp SVM.jar cs449g1/data-pod:/data/app/SVM.jar

echo "Running distributed SGD with $WORKERS workers"
$SPARK_HOME/bin/spark-submit --class main.Main --properties-file Spark/spark_conf --conf spark.executor.instances=${WORKERS} local:///data/app/SVM.jar
sh get_logs.sh svm logs/logs${WORKERS}
