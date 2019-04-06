SOURCES = $(shell find src/main/scala -name '*.scala')


all: build update run

build: $(SOURCES)
	sbt package

update: target/scala-2.11/project_2.11-0.1.jar
	kubectl cp target/scala-2.11/project_2.11-0.1.jar cs449g1/data-pod:/data/app/SVM.jar

run:
	sh run.sh $(workers) $(batch_size)

clean:
	rm -rf target/

delete:
	kubectl delete pods svm
	until kubectl get pod svm 2>&1 >/dev/null; do sleep 1; done