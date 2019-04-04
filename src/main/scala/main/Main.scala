package main

import java.lang.Integer.max

import org.apache.spark.{HashPartitioner, SparkConf, SparkContext, rdd}
import scala.util.control.Breaks._
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
    val dimensions: Int = data.map(x => x._2.keys.max).reduce(max) + 1

    // split data into train set and test set
    val full_data = data.join(labels)
    val train_proportion = 0.9
    val seed = 42

    full_data.cache()
    val split = full_data.randomSplit(Array(train_proportion, 1 - train_proportion), seed)
    val train_set = split(0)
    val test_set = split(1)

    val train_part = train_set.partitionBy(new HashPartitioner(workers))
    val test_part = test_set.partitionBy(new HashPartitioner(workers))

    val epochs = 10000
    val batchSize = sc.broadcast(128)
    val gamma = 0.03
    print("DIM: " + dimensions + ", " + full_data.getNumPartitions + "\n")

    var weights = Array.fill(dimensions)(0.0)

    val timesLog: Array[Long] = new Array[Long](4)

    //early stopping vars
    val patience = 1
    var previous_loss : Double = 1.1
    var patience_counter: Int = 0

    breakable {
      for (e <- 0 to epochs) {
        val batch_weight = sc.broadcast(weights)
        timesLog(0) = System.nanoTime()
        val grads = train_part.mapPartitions(p => {
          val shuffledP = Random.shuffle(p)
          val batch = shuffledP.take(batchSize.value)
          val w = batch_weight.value
          val grad = batch.map(i => {
            val (_, (x, y)) = i
            svm(x, y, w)
          })
          print("GRAD VALUE :" + grad.toString())
          grad
        }).collect()
        val gradsAug = grads.map(x => x.map { case (k, v) => k -> (v, 1) })

        timesLog(1) = System.nanoTime()

        val unNormalG = gradsAug.par.aggregate(Map[Int, (Double, Int)]())(merge, merge)
        val g = unNormalG.map { case (k, v) => k -> v._1 / v._2 }
        timesLog(2) = System.nanoTime()
        weights = update_weight(weights, g, gamma)
        timesLog(3) = System.nanoTime()
        val l = train_part.map(p => loss(p._2._1, p._2._2, weights)).mean()
        val val_loss = test_part.map(p => loss(p._2._1, p._2._2, weights)).mean()
        print("EPOCH " + e)
        for (elem <- timesLog) {
          print(" ; " + elem / 1000000000.0 + " ; ")
        }
        print("train loss :" + l)
        print("test loss: " + val_loss)

        //early stopping
        if(val_loss > previous_loss && patience_counter == patience){
            break
        }else{
          if(val_loss > previous_loss){
            patience_counter = patience_counter + 1
          }else{
            patience_counter = 1
          }
          previous_loss = val_loss
        }
      }
    }
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
