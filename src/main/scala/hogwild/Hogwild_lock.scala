package hogwild

import java.io.FileWriter
import java.util.concurrent.Executors

import hogwild.Data.{load_data, test_accuracy}
import main.Parameters._
import svm.SVM._

import scala.util.Random

object Hogwild_lock {

  var best_loss: Double = Double.MaxValue
  var patience_counter: Int = 0

  def run(workers: Int, batch_size: Int): Unit = {
    val pool = Executors.newFixedThreadPool(workers)

    val fileName = LOG_PATH + "/lock/" + workers + "_" + batch_size
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers, batch_size)

    val (d, dimensions) = load_data(DATA_PATH)
    val random = new Random(SEED)
    val data = random.shuffle(d).toArray
    val N = (data.length * VALIDATION_RATIO).toInt
    val (validation_set, train_set) = data.splitAt(N)

    // Initialization and broadcast of the variables
    val weights = Array.fill(dimensions)(0.0)
    val indices = splitRange(train_set.indices, workers)
    @volatile var done = false

    def runner(id: Int): Unit = {
      var ind: Stream[Int] = indices(id)
      while (!done) {
        weights.synchronized() {
          val w = weights.clone()
        }
        val i = ind.take(batch_size)
        ind = ind.drop(batch_size)
        val g = i.map(train_set(_)).map(p => svm(p._2._1, p._2._2, w, LAMBDA)).map(x => x.map { case (k, v) => k -> (v, 1) }).reduce(merge)
        update_weight(weights, g, LEARNING_RATE, LAMBDA, batch_size, workers)
        weights.synchronized() {
          val wu = weights.clone()
        }
        //val losses = train_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA))
        //val tl = losses.sum / losses.length

        val val_loss = validation_set.map(p => loss(p._2._1, p._2._2, wu, LAMBDA))
        val vl = val_loss.sum / N
        log(logfile, "SUMMARY(vl=" + vl + ")")
        if (early_stop(vl)) {
          done = true
        }
      }
    }

    val threads = (0 until workers).map(i => new Thread() {
      override def run(): Unit = {
        runner(i)
      }
    })

    threads.foreach(_.start())
    threads.foreach(_.join())

    //Final computation of the accuracy at the end of all computations
    val (acc, pos_acc, neg_acc) = test_accuracy(DATA_PATH, weights)
    log(logfile, "ACCURACY(acc=" + acc + ",+acc=" + pos_acc + ",-acc=" + neg_acc + ")")
    logfile.flush()
  }

  def log(logfile: FileWriter, message: String): Unit = {
    logfile.append("SVM " + message + " " + System.nanoTime() + "\n")
  }

  def logParams(logfile: FileWriter, workers: Int, batch_size: Int): Unit = {
    logfile.append("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",BATCH_SIZE=" + batch_size + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

  def update_weight(weights: Array[Double], gradient: Map[Int, (Double, Int)], gamma: Double, lambda: Double, batch_size: Double, workers: Int): Unit = {
    weights.synchronized() {
      for (i <- gradient.keys) {
        val g = gradient(i)
        weights(i) -= gamma * (g._1 / g._2)
      }
    }
  }

  def splitRange(r: Range, chunks: Int): Array[Stream[Int]] = {
    val nchunks = scala.math.max(chunks, 1)
    val chunkSize = scala.math.max(r.length / nchunks, 1)
    val starts = r.by(chunkSize).take(nchunks)
    val ends = starts.map(_ - 1).drop(1) :+ r.end
    starts.zip(ends).map(x => Stream.continually(Stream.range(x._1, x._2)).flatten).toArray
  }

  def merge(m1: Map[Int, (Double, Int)], m2: Map[Int, (Double, Int)]): Map[Int, (Double, Int)] = {
    m2 ++ m1.map {
      case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0, 0))._1, v._2 + m2.getOrElse(k, (0.0, 0))._2)
    }
  }

  def early_stop(vl: Double): Boolean = {
    if (vl > best_loss && patience_counter == PATIENCE) {
      true
    } else {
      if (vl > best_loss - EARLY_STOP_THRESHOLD) {
        patience_counter = patience_counter + 1
      } else {
        patience_counter = 1
        best_loss = vl
      }
      false
    }
  }

}
