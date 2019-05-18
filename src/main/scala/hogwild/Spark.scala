package hogwild

import java.io.FileWriter

import hogwild.Data.{loadDataRDD, testAccuracy}
import hogwild.Log.log
import hogwild.Parameters._
import hogwild.SVM._
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

import scala.util.Random
import scala.util.control.Breaks.{break, breakable}

/**
  * An object giving static methods to run Hogwild on a cluster using Spark.
  */
object Spark {

  /**
    * Runs the Hogwild SVM with the given parameters.
    *
    * @param batch_size the batch size
    */
  def run(batch_size: Int): Unit = {

    val conf = new SparkConf()
    val sc = new SparkContext(conf)

    val workers = conf.getInt("spark.executor.instances", 1)

    val logfileName = LOG_PATH + "/spark/" + workers + "_" + batch_size
    val logfile = new FileWriter(logfileName, false)
    log(logfile, workers, batch_size)

    val (data, dimensions) = loadDataRDD(sc, DATA_PATH)
    val split = data.randomSplit(Array(1 - VALIDATION_RATIO, VALIDATION_RATIO), SEED)
    val trainSet = split(0).partitionBy(new HashPartitioner(workers))
    val validationSet = split(1).partitionBy(new HashPartitioner(workers))

    val bs = sc.broadcast(batch_size)
    var weights = Array.fill(dimensions)(0.0)
    var best_loss = Double.MaxValue
    var patience_counter = 0

    breakable {
      for (e <- 0 to EPOCHS) {
        log(logfile, e, "START")

        val batchWeight = sc.broadcast(weights)
        val grads = trainSet.mapPartitions(p => {
          val batch = Random.shuffle(p).take(bs.value)
          batch.map(sparseGradient(_, batchWeight.value, LAMBDA))
        }).collect()

        log(logfile, e, "GRADIENTS_COMPUTED")

        val gradient = grads.par.map(expandSparseGradient).aggregate(Map[Int, (Double, Int)]())(mergeSparseGradient, mergeSparseGradient)

        log(logfile, e, "GRADIENTS_MERGED")

        weights = updateWeight(weights, gradient, LEARNING_RATE)

        log(logfile, e, "WEIGHTS_UPDATED")

        val trainLoss = trainSet.map(loss(_, weights, LAMBDA)).mean()
        val validationLoss = validationSet.map(loss(_, weights, LAMBDA)).mean()

        log(logfile, e, "SUMMARY(tl=" + trainLoss + ",vl=" + validationLoss + ")")

        if (validationLoss > best_loss && patience_counter == PATIENCE) {
          break
        } else {
          if (validationLoss > best_loss - EARLY_STOP_THRESHOLD) {
            patience_counter = patience_counter + 1
          } else {
            patience_counter = 1
            best_loss = validationLoss
          }
        }
      }
    }

    val (acc, pos_acc, neg_acc) = testAccuracy(sc, DATA_PATH, weights)
    log(logfile, "ACCURACY(acc=" + acc + ",+acc=" + pos_acc + ",-acc=" + neg_acc + ")")
    logfile.flush()
  }
}
