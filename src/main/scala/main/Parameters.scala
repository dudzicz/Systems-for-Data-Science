package main

object Parameters {
  val EPOCHS = 10000
  val LEARNING_RATE = 0.003
  val PATIENCE = 25
  val LAMBDA = 1e-5
  val VALIDATION_RATIO = 0.1
  val SEED = 42
  val DATA_PATH = "data/"
  val LOG_PATH = "logs"
  val SELECT_LABEL = "CCAT"
  val EARLY_STOP_THRESHOLD = 0.0001
}
