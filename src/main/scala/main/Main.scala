package main

import distributed.Distributed
import hogwild.Hogwild

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      throw new IllegalArgumentException("Usage: java -jar <mode>")
    }
    val mode = args(0)
    if (mode.equals("hogwild")) {
      if (args.length != 3){
        println("Usage : java -jar SVM.jar hogwild <workers> <batch_size>")
      }
      Hogwild.main(args(1).toInt, args(2).toInt)
    } else if (mode.equals("distributed")) {
      if (args.length != 2){
        println("Usage : java -jar SVM.jar distributed <batch_size>")
      }
      Distributed.run(args(1).toInt)
    } else {
      throw new IllegalArgumentException()
    }

  }

}
