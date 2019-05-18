package hogwild

/**
  * An object handling the data and SVM parameters.
  */
object Parameters {
  val DATA_PATH = "datasets/"
  val LOG_PATH = "log/"
  val SELECT_LABEL = "CCAT"

  val EPOCHS = 10000
  val LEARNING_RATE = 0.06
  val PATIENCE = 10
  val LAMBDA = 1e-5
  val VALIDATION_RATIO = 0.1
  val EARLY_STOP_THRESHOLD = 0.0001

  val SEED = 42

}
