# Systems for Data Science (CS-449) - Project Milestone 2
## GROUP 1 - Vincent Coriou, Damian Dudzicz, Karthigan Sinnathamby

### Project Description

This constitutes the code to run the our implementation of the EPFL CS-449 course's project. The code is based on work of Bifano, Bachmann and Allemann [hogwild-python](https://github.com/liabifano/hogwild-python/) and the paper [HOGWILD!](https://arxiv.org/abs/1106.5730).

### Setup Requirements

In order to compile and run the program the following should be installed on the user's systems.

1. Java version 8
2. Scala version 2.11.8
3. Spark version 2.4.0
4. SBT version 1.2.8

It is also required to define the environment variable ```$SPARK_HOME``` to the absolute path of spark in the

### Source Code Structure

```src/main/scala/hogwild``` contains the source code of the implementation
* ```Data.scala``` contains the methods to load and parse the Data
* ```Hogwild.scala``` contains the methods to run the local implementation of Hogwild (milestone 2)
* ```Log.scala``` contains the methods used to log events throughout the execution of the training
* ```Main.scala``` contains the main runnable method that interprets and parses the given program arguments
* ```Parameters.Scala``` contains constants and parameters
* ```SVM.scala``` contains the machine learning computations functions for the SVM
* ```Spark.scala``` contains the methods to run the Spark implementation of Hogwild (milestone 1)


### How to Run the Project

#### Spark version

In order to create the pod used to upload data to the cluster use the following command:

```
kubectl create -f Kubernetes/data_pod.yaml
```

In order to run the program on the cluster, use the following commands:

```
make
sh run-cluster.sh <workers> <batch_size>
```

with `<workers>` and `<batch_size>` being the desired number of workers and batch size respectively.

#### Local version

The local version can be ran using the following command:

```
make build
sh run-local.sh <mode> <workers> <batch_size>
```

with `<mode>` being the desired running mode (one of `hogwild` or `lock`), `<workers>` and `<batch_size>` being the desired number of workers and batch size respectively.

__Note__: By default the program assumes that the dataset is placed in a `<current_directory>/datasets`. This can be modified in the `Parameters.scala` file