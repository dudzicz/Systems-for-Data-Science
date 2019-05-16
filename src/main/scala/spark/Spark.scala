package spark

import java.io.FileWriter

import spark.Data.{load_data, test_accuracy}
import main.Parameters._
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}
import svm.SVM._

import scala.util.Random
import scala.util.control.Breaks._

object Spark {

  def run(batch_size: Int): Unit = {

    val conf = new SparkConf()
    val sc = new SparkContext(conf)

    //We get the number of workers from the execution command line directly in order to have a coherent number of nodes
    //with our system parameters
    val workers = conf.getInt("spark.executor.instances", 1)

    val fileName = LOG_PATH + "/spark/" + workers + "_" + batch_size
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers, batch_size)

    //Load the train dataset into an RDD
    val (data, dimensions) = load_data(sc, DATA_PATH)
    data.cache()

    //Splitting in train and validation set
    val split = data.randomSplit(Array(1 - VALIDATION_RATIO, VALIDATION_RATIO), SEED)

    //Partition of the dataset among the workers nodes
    val train_set = split(0).partitionBy(new HashPartitioner(workers))
    val validation_set = split(1).partitionBy(new HashPartitioner(workers))

    // Initialization and broadcast of the variables
    val bs = sc.broadcast(batch_size)
    var weights = Array.fill(dimensions)(0.0)
    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0

    //Break required for the early stopping
    breakable {
      for (e <- 0 to EPOCHS) {
        log(logfile, e, "START")

        //Update the weights among all workers nodes
        val batch_weight = sc.broadcast(weights)

        //Gradient comuptation on all workers nodes
        val grads = train_set.mapPartitions(p => {

          //Shuffle to avoid reusing the same part of the partition for each computation
          val batch = Random.shuffle(p).take(bs.value)
          val w = batch_weight.value
          val grad = batch.map(i => {
            val (_, (x, y)) = i
            svm(x, y, w, LAMBDA)
          })
          grad
        }).collect()

        val gradsAug = grads.map(x => x.map { case (k, v) => k -> (v, 1) })

        log(logfile, e, "GRADIENTS_COMPUTED")

        //Optimized merge of the gradients
        val unNormalG = gradsAug.par.aggregate(Map[Int, (Double, Int)]())(merge, merge)
        val computedGrad = unNormalG.map { case (k, v) => k -> v._1 / v._2 }

        log(logfile, e, "GRADIENTS_MERGED")

        //Update of the weights with new computed gradients
        weights = update_weight(weights, computedGrad, LEARNING_RATE)

        log(logfile, e, "WEIGHTS_UPDATED")

        //losses computations
        val train_loss = train_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()
        val val_loss = validation_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA)).mean()

        log(logfile, e, "SUMMARY(tl=" + train_loss + ",vl=" + val_loss + ")")

        //Early stopping
        if (val_loss > best_loss && patience_counter == PATIENCE) {
          break
        } else {
          if (val_loss > best_loss - EARLY_STOP_THRESHOLD) {
            patience_counter = patience_counter + 1
          } else {
            patience_counter = 1
            best_loss = val_loss
          }
        }
      }
    }

    //Final computation of the accuracy at the end of all computations
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

  def logParams(logfile: FileWriter, workers: Int, batch_size: Int): Unit = {
    logfile.append("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",BATCH_SIZE=" + batch_size + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

}
