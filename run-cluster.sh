#!/bin/bash

check_pod() {
    kubectl get pods ${1} 2>&1 > /dev/null
    return $?
}

check_pod data-pod

delete_pod () {
    POD=${1}
    echo "Deleting pod \"${POD}\""
    kubectl delete pods ${POD} 2>&1 > /dev/null
    while
        check_pod ${POD}
        [[ $? -eq 0 ]]
    do
        sleep 1
    done
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

fetch_log() {
    echo "Fetching logs..."
    check_pod ${1}
    if [[ $? -ne 0 ]]
    then
        create_pod data-pod Kubernetes/data_pod.yaml
    fi
    kubectl cp cs449g1/data-pod:/data/log/ logs/
}


if [[ $# -ne 2 ]]
then
    echo "Usage ${0} <workers> <batch_size>"
    exit
fi

WORKERS=${1}
BATCH_SIZE=${2}
delete_pod svm-${WORKERS}-${BATCH_SIZE}
echo "Running distributed SGD with $WORKERS workers and $BATCH_SIZE batch size"
${SPARK_HOME}/bin/spark-submit --class hogwild.Main --properties-file Spark/spark_conf --conf spark.executor.instances=${WORKERS} --conf spark.kubernetes.driver.pod.name=svm-${WORKERS}-${BATCH_SIZE} local:///data/app/SVM.jar spark ${BATCH_SIZE}
fetch_log