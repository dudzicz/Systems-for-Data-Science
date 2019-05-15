package hogwild

import java.io.FileWriter
import java.util.concurrent.{CountDownLatch, Executors}
import hogwild.Data.{load_data, test_accuracy}
import main.Parameters._

import scala.util.Random
import scala.util.control.Breaks._
import scala.collection.concurrent.TrieMap
import svm.SVM._

object Hogwild {

  def run(workers: Int, batch_size: Int): Unit = {
    val pool = Executors.newFixedThreadPool(workers)

    val fileName = LOG_PATH + "/hogwild/" + workers + "_" + batch_size
    val logfile = new FileWriter(fileName, false)
    logParams(logfile, workers, batch_size)

    val (d, dimensions) = load_data(DATA_PATH)
    val random = new Random(SEED)
    val data = random.shuffle(d).toArray
    val N = (data.length * VALIDATION_RATIO).toInt
    val (validation_set, train_set) = data.splitAt(N)

    // Initialization and broadcast of the variables
    val weights = Array.fill(dimensions)(0.0)
    var best_loss: Double = Double.MaxValue
    var patience_counter: Int = 0
    var counter = new CountDownLatch(workers * batch_size)
    val indices = splitRange(train_set.indices, workers)

    var gradient = new TrieMap[Int,(Double,Int)]()

    @volatile var done = false
    for (w <- 0 until workers) {
      pool.execute(new Runnable {
        var ind = indices(w)

        override def run(): Unit = {
          while (!done) {
            val i = ind.head
            ind = ind.tail
            val (_, (x, y)) = train_set(i)
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
        gradient = new TrieMap[Int,(Double,Int)]()
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
    logfile.flush()
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

  def update_grad(gradient: TrieMap[Int,(Double, Int)], delta: Map[Int, Double]): Unit = {
    for ((a, b) <- delta) {
      val g = gradient.getOrElse(a,(0.0,0))
      gradient.update(a, (g._1 + b, g._2 + 1))
    }
  }

  def update_weight(weights: Array[Double], gradient: TrieMap[Int,(Double, Int)], gamma: Double, lambda: Double, batch_size: Double, workers: Int): Unit = {
    for (i <- gradient.keys) {
      val g = gradient(i)
        weights(i) -= gamma * (g._1 / g._2)
    }
  }

  def splitRange(r: Range, chunks: Int): Array[Stream[Int]] = {
    val nchunks = scala.math.max(chunks, 1)
    val chunkSize = scala.math.max(r.length / nchunks, 1)
    val starts = r.by(chunkSize).take(nchunks)
    val ends = starts.map(_ - 1).drop(1) :+ r.end
    starts.zip(ends).map(x => Stream.continually(Stream.range(x._1, x._2)).flatten).toArray
  }

}
