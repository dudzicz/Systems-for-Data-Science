#!/usr/bin/env bash

docker build -t dudzicz/cs449g1:latest -f Docker/Dockerfile .
docker push dudzicz/cs449g1:latest
$SPARK_HOME/bin/spark-submit --master k8s://https://10.90.36.16:6443 --deploy-mode cluster --name sparktest --class main.Main --conf spark.executor.instances=5 --conf spark.kubernetes.namespace=cs449g1 --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark --conf spark.kubernetes.driver.pod.name=sparkpodtest1 --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.myvolume.options.claimName=cs449g1-scratch --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.myvolume.options.claimName=cs449g1-scratch --conf spark.kubernetes.driver.volumes.persistentVolumeClaim.myvolume.mount.path=/data --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.myvolume.mount.path=/data --conf spark.kubernetes.container.image=dudzicz/cs449g1:latest  local:///SVM.jar
kubectl logs sparkpodtest1
kubectl delete pods sparkpodtest1