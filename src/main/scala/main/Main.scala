package main

import java.io.FileWriter

import main.Data.{load_train, test_accuracy}
import main.Parameters._
import main.SVM.{loss, svm, update_weight}
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

import scala.util.Random
import scala.util.control.Breaks._

object Main {


  def main(args: Array[String]): Unit = {
    if (args.length > 1) {
      throw new IllegalArgumentException()
    } else if (args.length == 1) {
      BATCH_SIZE = args(0).toInt
    }


    val conf = new SparkConf()
    val sc = new SparkContext(conf)

    val workers = conf.getInt("spark.executor.instances", 1)
    val fileName = "/data/log/" + workers + "_" + BATCH_SIZE
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers)
    val (data, dimensions) = load_train(sc, DATA_PATH)

    data.cache()
    val split = data.randomSplit(Array(1 - VALIDATION_RATIO, VALIDATION_RATIO), SEED)
    val train_set = split(0).partitionBy(new HashPartitioner(workers))
    val validation_set = split(1).partitionBy(new HashPartitioner(workers))

    load_train(sc, DATA_PATH)

    val batchSize = sc.broadcast(BATCH_SIZE)
    var weights = Array.fill(dimensions)(0.0)

    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0

    breakable {
      for (e <- 0 to EPOCHS) {
        log(logfile, e, "START")
        val batch_weight = sc.broadcast(weights)
        val grads = train_set.mapPartitions(p => {
          val batch = Random.shuffle(p).take(batchSize.value)
          val w = batch_weight.value
          val grad = batch.map(i => {
            val (_, (x, y)) = i
            svm(x, y, w, LAMBDA)
          })
          grad
        }).collect()
        val gradsAug = grads.map(x => x.map { case (k, v) => k -> (v, 1) })

        log(logfile, e, "GRADIENTS_COMPUTED")

        val unNormalG = gradsAug.par.aggregate(Map[Int, (Double, Int)]())(merge, merge)
        val g = unNormalG.map { case (k, v) => k -> v._1 / v._2 }

        log(logfile, e, "GRADIENTS_MERGED")

        weights = update_weight(weights, g, LEARNING_RATE)

        log(logfile, e, "WEIGHTS_UPDATED")

        val train_loss = train_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()
        val val_loss = validation_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()

        log(logfile, e, "SUMMARY(tl=" + train_loss + ",vl=" + val_loss + ")")

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
    val (acc, pos_acc, neg_acc) = test_accuracy(sc, DATA_PATH, weights)
    log(logfile, "ACCURACY(acc=" + acc + ",+acc=" + pos_acc + ",-acc=" + neg_acc + ")")
    logfile.flush()
  }

  def log(logfile: FileWriter, message: String): Unit = {
    logfile.append("SVM " + message + " " + System.nanoTime() + "\n")
  }

  def log(logfile: FileWriter, epoch: Int, message: String): Unit = {
    logfile.append("SVM EPOCH(" + epoch + ") " + message + " " + System.nanoTime() + "\n")
  }

  def logParams(logfile: FileWriter, workers: Int): Unit = {
    logfile.append("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",BATCH_SIZE=" + BATCH_SIZE + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

  def merge(m1: Map[Int, (Double, Int)], m2: Map[Int, (Double, Int)]): Map[Int, (Double, Int)] = {
    m2 ++ m1.map {
      case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0, 0))._1, v._2 + m2.getOrElse(k, (0.0, 0))._2)
    }
  }

}
