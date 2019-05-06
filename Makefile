SOURCES = $(shell find src/main/scala -name '*.scala')


all: build update

data-pod:
	kubectl create -f Kubernetes/data_pod.yaml

build: $(SOURCES)
	sbt package

update: target/scala-2.11/project_2.11-0.1.jar
	kubectl cp target/scala-2.11/project_2.11-0.1.jar cs449g1/data-pod:/data/app/SVM.jar

clean:
	rm -rf target/

fetch_logs:
