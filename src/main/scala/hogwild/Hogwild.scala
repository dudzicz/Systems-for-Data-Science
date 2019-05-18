package hogwild

import java.io.FileWriter

import hogwild.Data.{loadData, testAccuracy}
import hogwild.Log.log
import hogwild.Parameters._
import hogwild.SVM._

import scala.concurrent.Lock
import scala.util.Random

/**
  * An object giving static methods to run Hogwild an a local machine.
  */
object Hogwild {

  /**
    * Runs the hogwild SVM with the given parameters.
    *
    * @param workers   the number of workers to use
    * @param batchSize the batch size
    * @param locking   whether to use locking or not
    */
  def run(workers: Int, batchSize: Int, locking: Boolean): Unit = {
    val random = new Random(SEED)

    val logfileName = LOG_PATH + "/hogwild/" + workers + "_" + batchSize
    val logfile = new FileWriter(logfileName, false)
    log(logfile, workers, batchSize)

    val (data, dimensions) = loadData(DATA_PATH)
    val validationSize = (data.length * VALIDATION_RATIO).toInt
    val (validationSet, trainSet) = random.shuffle(data).toArray.splitAt(validationSize)

    val weights = Array.fill(dimensions)(0.0)
    val workerIndices = splitRange(trainSet.indices, workers)
    val lock = new Lock()
    @volatile var done = false

    var bestLoss = Double.MaxValue
    var patienceCounter = 0

    /**
      * Decides the early stopping of the model training.
      *
      * @param validationLoss the last validation loss
      * @return true if the model training should stop, false otherwise
      */
    def early_stop(validationLoss: Double): Boolean = {
      if (validationLoss > bestLoss && patienceCounter == PATIENCE) {
        true
      } else {
        if (validationLoss > bestLoss - EARLY_STOP_THRESHOLD) {
          patienceCounter = patienceCounter + 1
        } else {
          patienceCounter = 1
          bestLoss = validationLoss
        }
        false
      }
    }

    /**
      * Defines the workers' execution flow.
      *
      * @param id the id of the worker
      */
    def runner(id: Int): Unit = {
      var indices = workerIndices(id)
      while (!done) {
        val batch = indices.take(batchSize).map(trainSet(_))
        indices = indices.drop(batchSize)

        if (locking) lock.acquire()
        val workerWeights = weights.clone()
        if (locking) lock.release()


        val gradient = batch.map(sparseGradient(_, workerWeights, LAMBDA)).map(expandSparseGradient).reduce(mergeSparseGradient)

        if (locking) lock.acquire()
        updateWeight(weights, gradient, LEARNING_RATE)
        val validationWeights = weights.clone()
        if (locking) lock.release()

        val validationLoss = validationSet.map(loss(_, validationWeights, LAMBDA)).sum / validationSize

        if (early_stop(validationLoss)) {
          done = true
        }

        log(logfile, "SUMMARY(vl=" + validationLoss + ")")
      }
    }

    val threads = (0 until workers).map(i => new Thread() {
      override def run(): Unit = {
        runner(i)
      }
    })

    threads.foreach(_.start())
    threads.foreach(_.join())

    val (acc, pos_acc, neg_acc) = testAccuracy(DATA_PATH, weights)
    log(logfile, "ACCURACY(acc=" + acc + ",+acc=" + pos_acc + ",-acc=" + neg_acc + ")")
    logfile.flush()
  }

  /**
    * Splits the given range into equal size chunks.
    *
    * @param range the range to split
    * @param n     the number of chunks
    * @return an Array of infinite Stream on the chunked ranges
    */
  def splitRange(range: Range, n: Int): Array[Stream[Int]] = {
    val nchunks = scala.math.max(n, 1)
    val chunkSize = scala.math.max(range.length / nchunks, 1)
    val starts = range.by(chunkSize).take(nchunks)
    val ends = starts.map(_ - 1).drop(1) :+ range.end
    starts.zip(ends).map { case (start, end) => Stream.continually(Stream.range(start, end)).flatten }.toArray
  }

}
