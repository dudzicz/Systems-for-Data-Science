package hogwild

import java.io.FileWriter
import java.util.concurrent.{CountDownLatch, Executors}

import distributed.SVM.{loss, svm}
import hogwild.Data.{load_data, test_accuracy}
import main.Parameters._

import scala.util.Random
import scala.util.control.Breaks._

object Hogwild {

  def main(workers: Int, batch_size: Int): Unit = {
    val pool = Executors.newFixedThreadPool(workers)

    val fileName = LOG_PATH + "/hogwild/" + workers + "_" + batch_size
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers, batch_size)
    val (d, dimensions) = load_data(DATA_PATH)
    val random = new Random(SEED)
    val data = random.shuffle(d).toArray
    val N = (data.length * VALIDATION_RATIO).toInt
    val (train_set, validation_set) = data.splitAt(N)

    // Initialization and broadcast of the variables
    var weights = Array.fill(dimensions)(0.0)
    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0
    var counter = new CountDownLatch(workers * batch_size)

    var gradient = Array.fill(dimensions)((0.0, 0))
    @volatile var done = false
    for (w <- 0 until workers) {
      pool.execute(new Runnable {
        override def run(): Unit = {
          while (!done) {
            val i = random.nextInt(N)
            val (_, (x, y)) = data(i)
            val g = svm(x, y, weights, LAMBDA)
            update_grad(gradient, g)
            counter.countDown()
          }
        }
      })
    }


    breakable {
      for (e <- 0 to EPOCHS) {
        counter.await()
        val g = gradient.clone()
        gradient = Array.fill(dimensions)((0.0, 0))
        counter = new CountDownLatch(workers * batch_size)
        update_weight(weights, g, LEARNING_RATE, LAMBDA, batch_size, workers)
        log(logfile, e, "START")
        val losses = train_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA))
        val tl = losses.sum / losses.length

        val val_loss = validation_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA))
        val vl = val_loss.sum / val_loss.length
        log(logfile, e, "SUMMARY(tl=" + tl + ",vl=" + vl + ")")

        //Early stopping
        if (vl > best_loss && patience_counter == PATIENCE) {
          break
        } else {
          if (vl > best_loss - EARLY_STOP_THRESHOLD) {
            patience_counter = patience_counter + 1
          } else {
            patience_counter = 1
            best_loss = vl
          }
        }
      }
    }
    done = true
    pool.shutdown()
    //Final computation of the accuracy at the end of all computations
    val (acc, pos_acc, neg_acc) = test_accuracy(DATA_PATH, weights)
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

  def update_grad(gradient: Array[(Double, Int)], delta: Map[Int, Double]): Unit = {
    for ((a, b) <- delta) {
      val g = gradient(a)
      gradient(a) = (g._1 + b, g._2 + 1)
    }
  }

  def update_weight(weights: Array[Double], gradient: Array[(Double, Int)], gamma: Double, lambda: Double, batch_size: Double, workers: Int): Unit = {
    for (i <- weights.indices) {
      val g = gradient(i)
      if (g._2 != 0) {
        weights(i) -= gamma * (g._1 / g._2)
      }
    }
  }

}
