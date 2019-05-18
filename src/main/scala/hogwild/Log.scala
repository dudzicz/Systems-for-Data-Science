package hogwild

import java.io.FileWriter

import hogwild.Parameters.{EPOCHS, LEARNING_RATE, PATIENCE}

object Log {

  /**
    * Logs the given message into the logfile.
    *
    * @param logfile the logfile
    * @param message the message to log
    */
  def log(logfile: FileWriter, message: String): Unit = {
    logfile.append("SVM " + message + " " + System.nanoTime() + "\n")
  }

  /**
    * Logs the given message into the lgofile.
    *
    * @param logfile the logfile
    * @param epoch   the epoch
    * @param message the message to log
    */
  def log(logfile: FileWriter, epoch: Int, message: String): Unit = {
    logfile.append("SVM EPOCH(" + epoch + ") " + message + " " + System.nanoTime() + "\n")
  }

  /**
    * Logs the parameters of the algorithm into the logfile.
    *
    * @param logfile    the logfile
    * @param workers    the number of workers
    * @param batch_size the batch size
    */
  def log(logfile: FileWriter, workers: Int, batch_size: Int): Unit = {
    logfile.append("SVM PARAMETERS " + "WORKERS=" + workers + ",EPOCHS=" + EPOCHS + ",BATCH_SIZE=" + batch_size
      + ",LEARNING_RATE=" + LEARNING_RATE + ",PATIENCE=" + PATIENCE + "\n")
  }

}
