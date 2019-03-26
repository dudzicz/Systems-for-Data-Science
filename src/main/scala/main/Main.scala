package main
import java.lang.Integer.max

import org.apache.spark.{SparkConf, SparkContext}
import svm.SVM.svm

object Main {
  def main(args: Array[String]): Unit = {
    val appName = "g1"
    val master = "local"

    val conf = new SparkConf().setAppName(appName).setMaster(master)
    val sc = new SparkContext(conf)

    val datalines = sc.textFile("hdfs:///data/datasets/lyrl2004_vectors_train.dat")
    val topics = sc.textFile("hdfs:///data/datasets/rcv1-v2.topics.qrels")

    val selected = "CCAT"

    val labels = topics.map(i => {
      val p = i.split("")
      p(1).toInt -> p(0)
    }).groupByKey().mapValues(i => {
      if (i.toList.contains(selected)) 1 else -1
    })


    val data = datalines.map(i => {
      val q = i.split("  ")
      val l = q(0).toInt
      val p = q(1).split(" ")
      val x = p.map(j => {
        val p = j.split(":")
        p(0).toInt -> p(1).toDouble
      }).toMap
      (l, x)
    })

    val full_data = data.join(labels)
    val dimensions: Int = data.map(x => x._2.keys.max).reduce(max)

    val epochs = 10
    val batchSize = 128
    val gamma = 0.01

    var weights = Array.fill(dimensions)(0.0)

    for (e <- 0 to epochs) {
      val g = full_data.map(p => {
        val x = p._2._1
        val y = p._2._2
        svm(x, y, weights).toSeq
      }).reduce((p, q) => {
        p ++ q
      }).groupBy(_._1).mapValues(p => p.map(_._2).sum)
      for ((a, b) <- g) {
        weights(a) -= gamma * b
      }
    }
  }
}
