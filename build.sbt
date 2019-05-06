name := "Project"

version := "0.1"

scalaVersion := "2.11.8"

mainClass in Compile := Some("main.Main")

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "com.google.guava" % "guava" % "11.0.2"