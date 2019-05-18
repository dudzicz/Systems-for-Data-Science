package hogwild

object Main {
  /**
    * Main function parsing the arguments.
    *
    * @param args the program's argument
    */
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: scala SVM.jar <mode>")
    } else {
      val mode = args(0)
      if (mode.equals("spark")) {
        if (args.length != 2) {
          System.err.println("Usage: scala SVM.jar spark <batch_size>")
        }
        Spark.run(args(1).toInt)
      } else if (mode.equals("hogwild")) {
        if (args.length != 3) {
          System.err.println("Usage: scala SVM.jar hogwild <workers> <batch_size>")
        }
        Hogwild.run(args(1).toInt, args(2).toInt, locking = false)
      } else if (mode.equals("lock")) {
        if (args.length != 3) {
          System.err.println("Usage: scala SVM.jar lock <workers> <batch_size>")
        }
        Hogwild.run(args(1).toInt, args(2).toInt, locking = true)
      } else {
        System.err.println("Invalid mode: " + mode)
      }
    }
  }
}
