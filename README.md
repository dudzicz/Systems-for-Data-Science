# Systems for Data Science (CS-449) - Project Milestone 1
## GROUP 1 - Vincent Coriou, Damian Dudzicz, Karthigan Sinnathamby

### Project Description

This constitutes the code to run the implementation in Spark of the first milestone of the EPFL CS-449 course. The code is based on work of Bifano, Bachmann and Allemann [hogwild-python](https://github.com/liabifano/hogwild-python/) and the paper [HOGWILD!](https://arxiv.org/abs/1106.5730).

### Setup Requirements

In order to compile and run the program the following should be installed on the user's systems.

1. Java version 8
2. Scala version 2.11.8
3. Spark version 2.4.0
4. SBT version 1.2.8

It is also required to define the environment variable ```$SPARK_HOME``` to the absolute path of spark in the

### Source Code Structure

```src/main/scala``` contains the source code of the implementaiton
* ```Data.scala``` contains the method to load and parse the Data
* ```main.Main.scala``` contains the core of the run and the computations
* ```Parameters.Scala``` holds constants and parameters
* ```SVM.scala``` contains the machine learning computations functions for the SVM

### How to Run the Project

In order to create the pod used to upload data to the cluster use the following command:

```
kubectl create -f Kubernetes/data_pod.yaml
```

In order to run the program on the cluster, use the following commands:

```
make workers=<workers> batch_size=<batch_size>
```

with `<workers>` and `<batch_size>` being the desired number of workers and batch size respectively.

In order to run the program with the same inputs we used for the report, use the following command:

```
sh run_all.sh
```
