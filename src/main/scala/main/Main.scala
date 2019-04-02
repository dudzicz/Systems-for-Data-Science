package main

import java.lang.Integer.max

import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}
import svm.SVM.{loss, svm}

import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {

    val appName = "g1"
    val master = "local"

    val conf = new SparkConf().setAppName(appName).setMaster(master)
    val sc = new SparkContext(conf)

    val workers = conf.getInt("spark.executor.instances", 1)

    val datalines = sc.textFile("file:///data/datasets/lyrl2004_vectors_train.dat")
    val topics = sc.textFile("file:///data/datasets/rcv1-v2.topics.qrels")

    val selected = "CCAT"

    val labels = topics.map(line => {
      val p = line.split(" ")
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

    val full_data = data.join(labels).partitionBy(new HashPartitioner(workers))
    val dimensions: Int = data.map(x => x._2.keys.max).reduce(max) + 1

    val epochs = 10
    val batchSize = sc.broadcast(128)
    val gamma = 0.03
    print("DIM: " + dimensions + ", " + full_data.getNumPartitions + "\n")

    var weights = Array.fill(dimensions)(0.0)

    var timesLog :Array[Long] = new Array[Long](4)
    for (e <- 0 to epochs) {
      val batch_weight = sc.broadcast(weights)
      timesLog(0) = System.nanoTime()
      val grads = full_data.mapPartitions(p => {
        val batch = p.take(batchSize.value)
        val w = batch_weight.value
        val grad = batch.map(i => {
          val (_, (x, y)) = i
          svm(x, y, w)
        })
        grad
      }).collect()
      val gradsAug = grads.map(x => x.map {case (k,v) => k -> (v,1)} )

      timesLog(1) = System.nanoTime()

      val unNormalG = gradsAug.par.aggregate(Map[Int,(Double,Int)]())(merge,merge)
      val g = unNormalG.map { case (k, v) => k -> v._1/v._2 }
      timesLog(2) = System.nanoTime()
      weights = update_weight(weights, g, gamma)
      timesLog(3) = System.nanoTime()
      val l = full_data.map(p => loss(p._2._1, p._2._2, weights)).mean()
      print("EPOCH " + e)
      for (elem <- timesLog) {print(" ; " + elem/1000000000.0 + " ; ")}
      print(l)
    }
  }

  def mergeMap(a: Map[Int, Double], b: Map[Int, Double]): Map[Int, Double] = {
    (a.toSeq ++ b.toSeq).groupBy(_._1).mapValues(p => p.map(_._2).sum).map(identity)
  }

  def update_weight(w: Array[Double], grad: Map[Int, Double], gamma: Double): Array[Double] = {
    for ((a, b) <- grad) {
      w(a) -= gamma * b
    }
    w
  }

  def merge(m1: Map[Int, (Double,Int)], m2: Map[Int, (Double, Int)]) : Map[Int, (Double,Int)] = {
    m2 ++ m1.map { case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0,0))._1, v._2 + m2.getOrElse(k, (0.0,0))._2)}
  }

}
