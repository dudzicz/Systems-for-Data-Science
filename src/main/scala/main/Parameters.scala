package main

object Parameters {
  val EPOCHS = 10000
  val LEARNING_RATE = 0.06
  val PATIENCE = 10
  val LAMBDA = 1e-5
  val VALIDATION_RATIO = 0.1
  val SEED = 42
  val DATA_PATH = "datasets/"
  val LOG_PATH = "local_log/"
  val SELECT_LABEL = "CCAT"
  val EARLY_STOP_THRESHOLD = 0.0001
}
