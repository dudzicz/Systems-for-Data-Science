package main

import distributed.Distributed
import hogwild.{Hogwild, Hogwild_lock}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      throw new IllegalArgumentException("Usage: scala SVM.jar <mode>")
    }
    val mode = args(0)
    if (mode.equals("hogwild")) {
      if (args.length != 3){
        println("Usage : scala SVM.jar hogwild <workers> <batch_size>")
      }
      Hogwild.run(args(1).toInt, args(2).toInt)
    } else if (mode.equals("distributed")) {
      if (args.length != 2){
        println("Usage : scala SVM.jar distributed <batch_size>")
      }
      Distributed.run(args(1).toInt)
    } else if (mode.equals("lock")) {
      if (args.length != 3){
        println("Usage : scala SVM.jar lock <workers> <batch_size>")
      }
      Hogwild_lock.run(args(1).toInt, args(2).toInt)
    } else {
      throw new IllegalArgumentException()
    }

  }

}
