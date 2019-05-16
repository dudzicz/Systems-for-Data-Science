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
    fetch_log
elif [[ ${MODE} = "hogwild" || ${MODE} = "lock" ]]
then
    delete_pod hogwild
    create_pod hogwild Kubernetes/hogwild.yaml
    echo "Running hogwild SGD with $WORKERS workers and $BATCH_SIZE batch size"
    kubectl exec -it hogwild -- scala -J-Xmx9g /data/app/SVM.jar ${MODE} ${WORKERS} ${BATCH_SIZE}
    delete_pod hogwild
    fetch_log
else
    echo "Invalid mode: ${MODE}. Expected \"distributed\" or \"hogwild\""
fi
