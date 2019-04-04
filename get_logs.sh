#!/bin/bash

kubectl logs ${1} | grep "SVM " > ${2}