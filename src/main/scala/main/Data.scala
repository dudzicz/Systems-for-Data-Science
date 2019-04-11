package main

import java.lang.Integer.max

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object Data {
  def load_train(sc: SparkContext, path: String): (RDD[(Int, (Map[Int, Double], Int))], Int) = {
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

  def load_test(sc: SparkContext, path: String): RDD[(Int, (Map[Int, Double], Int))] = {
    val l = 0 until 4
    val topics = sc.textFile(path + "rcv1-v2.topics.qrels")
    val rdds  = l.map(i => {
      val datalines = sc.textFile(path + "lyrl2004_vectors_test_pt" + i + ".dat")
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
      data.join(labels)
    })
    val data = rdds.reduce(_ ++ _)
    data
  }
}
