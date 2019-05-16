#!/usr/bin/env bash

APP_NAME=karna_hog
REPO=karna2605
KUBER_LOGIN=cs449g1

DATA_PATH=/data/datasets

while getopts ":n:r:w:" opt; do
  case $opt in
    n) N_WORKERS="$OPTARG";; # number workers
    r) RUNNING_MODE="$OPTARG";; # synchronous or asynchronous
    w) WHERE="$OPTARG";; # cluster or local
    \?) echo "Invalid option -$OPTARG" >&2
    ;;
  esac
done

if [[ $WHERE = "local" ]];
then
    bash run-local.sh -r $RUNNING_MODE
    exit 0
fi;


function shutdown_infra {
    if ! [[ -z $(kubectl get services | grep coordinator-service-bis) ]];
    then
        kubectl delete -f Kubernetes/coordinator-bis.yaml --cascade=true
    fi;

    if ! [[ -z $(kubectl get services | grep workers-service-bis) ]];
    then
        kubectl delete -f Kubernetes/workers-bis.yaml --cascade=true
    fi;

    if ! [[ -z $(kubectl get configmap | grep hogwild-config-bis) ]];
    then
        kubectl delete configmap hogwild-config-bis
    fi;
}

echo
echo "----- Logging into Docker Hub -----"
docker login --username=$DOCKER_USER --password=$DOCKER_PASS 2> /dev/null

echo
echo "----- Deleting remaining infra -----"
shutdown_infra

echo
echo "----- Building and Pushing docker to Docker Hub -----"
docker build -f `pwd`/Docker/Dockerfile `pwd` -t ${REPO}/${APP_NAME}
docker push ${REPO}/${APP_NAME}


echo
echo "----- Starting workers -----"
kubectl create configmap hogwild-config-bis --from-literal=replicas=${N_WORKERS} \
                                        --from-literal=running_mode=${RUNNING_MODE} \
                                        --from-literal=data_path=${DATA_PATH} \
                                        --from-literal=running_where=${WHERE}
sed "s/\(replicas:\)\(.*\)/\1 ${N_WORKERS}/" Kubernetes/workers_template-bis.yaml > Kubernetes/workers-bis.yaml
kubectl create -f Kubernetes/workers-bis.yaml


while [ $(kubectl get pods | grep worker-bis | grep Running | wc -l) != ${N_WORKERS} ]
do
    sleep 40
    kubectl get pods | grep worker-bis | grep -v Running | awk '{print $1}' | xargs kubectl delete pods
done


echo
echo "----- Workers are up and running, starting coordinator -----"
kubectl create -f Kubernetes/coordinator-bis.yaml


while [ $(kubectl get pods | grep coordinator-bis | grep Running | wc -l) == 0 ]
do
    sleep 1
done

echo
echo "----- Running Job -----"


MY_TIME="`date +%Y%m%d%H%M%S`" && kubectl cp coordinator-bis-0:log.json logs/log_${MY_TIME}.json 2> /dev/null
while [ $? -ne 0 ];
do
    sleep 0.1
    MY_TIME="`date +%Y%m%d%H%M%S`" && kubectl cp coordinator-bis-0:log.json logs/log_${MY_TIME}.json 2> /dev/null
done



echo
echo "----- Job Completed, writing log in logs/log_${MY_TIME}.json -----"


echo
echo "----- Shutting down infra -----"
#shutdown_infra
