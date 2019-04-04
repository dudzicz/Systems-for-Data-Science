package main

import java.lang.Integer.max

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object Data {
  def load_data(sc: SparkContext, path: String): (RDD[(Int, (Map[Int, Double], Int))], Int) = {
    val datalines = sc.textFile(path + "lyrl2004_vectors_train.dat")
    val topics = sc.textFile(path + "rcv1-v2.topics.qrels")

    val selected = Parameters.SELECT_LABEL

    val labels = topics.map(line => {
      val split = line.split(" ")
      split(1).toInt -> split(0)
    }).groupByKey().mapValues(label => {
      if (label.toList.contains(selected)) 1 else -1
    })


    val data = datalines.map(line => {
      val split = line.split("  ")
      val index = split(0).toInt
      val coords = split(1).split(" ")
      var map = coords.map(j => {
        val p = j.split(":")
        p(0).toInt -> p(1).toDouble
      }).toMap
      map += (0 -> 1)
      (index, map)
    })


    (data.join(labels), data.map(x => x._2.keys.max).reduce(max) + 1)
  }
}
