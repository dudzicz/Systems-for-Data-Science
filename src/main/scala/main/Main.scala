package main

import main.Data.load_data
import main.Parameters._
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}
import svm.SVM.{loss, svm, update_weight}

import scala.util.Random
import scala.util.control.Breaks._

object Main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    val sc = new SparkContext(conf)

    val workers = conf.getInt("spark.executor.instances", 1)
    logParams(workers)
    val (data, dimensions) = load_data(sc, DATA_PATH)

    data.cache()
    val split = data.randomSplit(Array(1 - VALIDATION_RATIO, VALIDATION_RATIO), SEED)
    val train_set = split(0).partitionBy(new HashPartitioner(workers))
    val test_set = split(1).partitionBy(new HashPartitioner(workers))

    val train_part = train_set
    val test_part = test_set

    val batchSize = sc.broadcast(BATCH_SIZE)
    var weights = Array.fill(dimensions)(0.0)

    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0

    breakable {
      for (e <- 0 to EPOCHS) {
        log(e, "START")
        val batch_weight = sc.broadcast(weights)
        val grads = train_part.mapPartitions(p => {
          val batch = Random.shuffle(p).take(batchSize.value)
          val w = batch_weight.value
          val grad = batch.map(i => {
            val (_, (x, y)) = i
            svm(x, y, w, LAMBDA)
          })
          grad
        }).collect()
        val gradsAug = grads.map(x => x.map { case (k, v) => k -> (v, 1) })

        log(e, "GRADIENTS_COMPUTED")

        val unNormalG = gradsAug.par.aggregate(Map[Int, (Double, Int)]())(merge, merge)
        val g = unNormalG.map { case (k, v) => k -> v._1 / v._2 }

        log(e, "GRADIENTS_MERGED")

        weights = update_weight(weights, g, LEARNING_RATE)

        log(e, "WEIGHTS_UPDATED")

        val train_loss = train_part.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()
        val val_loss = test_part.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()

        log(e, "SUMMARY(tl=" + train_loss + ",vl=" + val_loss + ")")

        //early stopping
        if (val_loss > best_loss && patience_counter == PATIENCE) {
          break
        } else {
          if (val_loss > best_loss) {
            patience_counter = patience_counter + 1
          } else {
            patience_counter = 1
            best_loss = val_loss
          }

        }
      }
    }
  }

  def log(epoch: Int, message: String): Unit = {
    print("SVM EPOCH(" + epoch + ") " + message + " " + System.nanoTime() + "\n")
  }

  def logParams(workers: Int): Unit = {
    print("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",BATCH_SIZE=" + BATCH_SIZE + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

  def merge(m1: Map[Int, (Double, Int)], m2: Map[Int, (Double, Int)]): Map[Int, (Double, Int)] = {
    m2 ++ m1.map { case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0, 0))._1, v._2 + m2.getOrElse(k, (0.0, 0))._2) }
  }

}
