package distributed

import java.lang.Integer.max

import main.Parameters
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import SVM.accuracy

object Data {

  //main.Main function to load properly the data and structure it in a fashion which can be handled easily
  def load_data(sc: SparkContext, path: String): (RDD[(Int, (Map[Int, Double], Int))], Int) = {
    val datalines = sc.textFile("file://" + path + "lyrl2004_vectors_train.dat")
    val topics = sc.textFile("file://" + path + "rcv1-v2.topics.qrels")

    val selected = Parameters.SELECT_LABEL

    //Transform the data to have a 1 or -1 associated with entries having the selected label
    val labels = topics.map(line => {
      val split = line.split(" ")
      split(1).toInt -> split(0)
    }).groupByKey().mapValues(label => {
      if (label.toList.contains(selected)) 1 else -1
    })

    val data = datalines.map(line => {
      val split = line.split("  ")
      val index = split(0).toInt
      val coords = split(1).split(" ")
      var map = coords.map(j => {
        val p = j.split(":")
        p(0).toInt -> p(1).toDouble
      }).toMap
      map += (0 -> 1)
      (index, map)
    })

    (data.join(labels), data.map(x => x._2.keys.max).reduce(max) + 1)
  }

  def test_accuracy(sc: SparkContext, path: String, weights: Array[Double]): (Double, Double, Double) = {
    val testIndex = 0 until 4
    val topics = sc.textFile("file://" + path + "rcv1-v2.topics.qrels")

    //Read the 4 test files and perform the accuracy computation
    val rdds = testIndex.map(i => {
      val datalines = sc.textFile("file://" + path + "lyrl2004_vectors_test_pt" + i + ".dat")
      val selected = Parameters.SELECT_LABEL
      val labels = topics.map(line => {
        val split = line.split(" ")
        split(1).toInt -> split(0)
      }).groupByKey().mapValues(label => {
        if (label.toList.contains(selected)) 1 else -1
      })

      val data = datalines.map(line => {
        val split = line.split("  ")
        val index = split(0).toInt
        val coords = split(1).split(" ")
        var map = coords.map(j => {
          val p = j.split(":")
          p(0).toInt -> p(1).toDouble
        }).toMap
        map += (0 -> 1)
        (index, map)
      })
      data.join(labels)
    })
    val (tp, tn, pos, neg) = rdds.map(accuracy(_, weights)).reduce((x, y) => (x._1 + y._1, x._2 + y._2, x._3 + y._3, x._4 + y._4))
    ((tp + tn).toDouble / (pos + neg), tp.toDouble / pos, tn.toDouble / neg)
  }
}
