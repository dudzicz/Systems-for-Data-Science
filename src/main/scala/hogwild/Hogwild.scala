package hogwild

import java.io.FileWriter
import java.util.concurrent.{CountDownLatch, Executors}

import com.google.common.util.concurrent.AtomicDoubleArray
import hogwild.Data.{load_data, test_accuracy}
import main.Parameters._
import svm.SVM.{loss, svm}

import scala.util.Random
import scala.util.control.Breaks._

object Hogwild {

  def main(workers: Int, batch_size: Int): Unit = {
    val pool = Executors.newFixedThreadPool(workers)

    val fileName = "/data/log/hogwild/" + workers + "_" + batch_size
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers)
    val (d, dimensions) = load_data(DATA_PATH)
    val random = new Random(SEED)
    val data = random.shuffle(d).toArray
    val N = (data.length * VALIDATION_RATIO).toInt
    val (train_set, validation_set) = data.splitAt(N)

    // Initialization and broadcast of the variables
    var weights = Array.fill(dimensions)(0.0)
    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0
    var counter = new CountDownLatch(batch_size)

    val gradient = new AtomicDoubleArray(dimensions)
    @volatile var done = false
    for (w <- 0 until workers) {
      pool.execute(new Runnable {
        override def run(): Unit = {
          while (!done) {
            val i = random.nextInt(N)
            val (_, (x, y)) = data(i)
            val g = svm(x, y, weights, LAMBDA)
            update_grad(gradient, g, LEARNING_RATE)
            counter.countDown()
          }
        }
      })
    }


    breakable {
      for (e <- 0 to EPOCHS) {
        counter.await()
        counter = new CountDownLatch(batch_size)
        log(logfile, e, "START")
        val losses = train_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA))
        val tl = losses.sum / losses.length

        val val_loss = validation_set.map(p => loss(p._2._1, p._2._2, weights, LAMBDA))
        val vl = val_loss.sum / val_loss.length

        log(logfile, e, "SUMMARY(tl=" + tl + ",vl=" + val_loss + ")")

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

  def logParams(logfile: FileWriter, workers: Int): Unit = {
    logfile.append("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

  def update_grad(gradient: AtomicDoubleArray, delta: Map[Int, Double], gamma: Double): Unit = {
    for ((a, b) <- delta) {
      gradient.addAndGet(a, -gamma * b)
    }
  }

}
