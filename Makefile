SOURCES = $(shell find src/main/scala -name '*.scala')


all: build update

build: $(SOURCES)
	sbt package
	cp target/scala-2.11/project_2.11-0.1.jar SVM.jar
	make clean

update: SVM.jar
	kubectl cp SVM.jar cs449g1/data-pod:/data/app/SVM.jar

clean:
	rm -rf target/
