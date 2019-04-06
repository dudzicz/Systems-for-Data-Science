#!/bin/sh

echo "Copying SVM.jar..."
kubectl cp SVM.jar cs449g1/data-pod:/data/app/SVM.jar