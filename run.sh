#!/bin/bash

delete_pod () {
    POD=${1}
    kubectl get pods ${POD} 2>&1 > /dev/null
    err=$?
    if [[ ${err} -eq 0 ]]
    then
        echo "Deleting existing pod \"${POD}\""
        kubectl delete pods ${POD} 2>&1 > /dev/null
        while kubectl get pod ${POD} 2>&1 > /dev/null
        do
            sleep 1
        done
    fi
}

create_pod () {
    POD=${1}
    echo "Creating pod \"${POD}\""
    FILE=${2}
    kubectl create -f ${FILE} 2>&1 > /dev/null
    while [[ -z $(kubectl get pod ${POD} | grep "Running") ]]
    do
        sleep 1
    done
}

if [[ $# -lt 3 ]]
then
    echo "Usage ${0} <mode> <workers> <batch_size>"
    exit
fi

MODE=${1}
WORKERS=${2}
BATCH_SIZE=${3}
if [[ ${MODE} = "distributed" ]]
then
    delete_pod svm-${WORKERS}-${BATCH_SIZE}
    echo "Running distributed SGD with $WORKERS workers and $BATCH_SIZE batch size"
    ${SPARK_HOME}/bin/spark-submit --class main.Main --properties-file Spark/spark_conf --conf spark.executor.instances=${WORKERS} --conf spark.kubernetes.driver.pod.name=svm-${WORKERS}-${BATCH_SIZE} local:///data/app/SVM.jar distributed ${BATCH_SIZE}
    kubectl cp cs449g1/data-pod:/data/log/${WORKERS}_${BATCH_SIZE} logs/${WORKERS}_${BATCH_SIZE}
elif [[ ${MODE} = "hogwild" ]]
then
    delete_pod hogwild
    create_pod hogwild Kubernetes/hogwild.yaml
    echo "Running hogwild SGD with $WORKERS workers and $BATCH_SIZE batch size"
    kubectl exec -it hogwild -- scala -J-Xmx16g /data/app/SVM.jar hogwild ${WORKERS} ${BATCH_SIZE}
else
    echo "Invalid mode: ${MODE}. Exected \"distributed\" or \"hogwild\""
fi
