package hogwild

import java.lang.Integer.max

import main.Parameters
import hogwild.SVM.predict

import scala.io.Source

object Data {

  //main.Main function to load properly the data and structure it in a fashion which can be handled easily
  def load_data(path: String): (List[(Int, (Map[Int, Double], Int))], Int) = {
    val datalines = Source.fromFile(path + "lyrl2004_vectors_train.dat").getLines().toList
    val topics = Source.fromFile(path + "rcv1-v2.topics.qrels").getLines().toList
    val selected = Parameters.SELECT_LABEL

    val labels = topics.map(line => {
      val split = line.split(" ")
      split(1).toInt -> split(0)
    }).groupBy(_._1).mapValues(label => {
      if (label.map(_._2).contains(selected)) 1 else -1
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

    (data.map(x => (x._1, (x._2, labels(x._1)))), data.map(x => x._2.keys.max).reduce(max) + 1)
  }

  def test_accuracy(path: String, weights: Array[Double]): (Double, Double, Double) = {
    val testIndex = 0 until 4
    val topics = Source.fromFile(path + "rcv1-v2.topics.qrels").getLines().toList

    //Read the 4 test files and perform the accuracy computation
    val lists = testIndex.map(i => {
      val datalines = Source.fromFile(path + "lyrl2004_vectors_test_pt" + i + ".dat").getLines().toList
      val selected = Parameters.SELECT_LABEL
      val labels = topics.map(line => {
        val split = line.split(" ")
        split(1).toInt -> split(0)
      }).groupBy(_._1).mapValues(label => {
        if (label.map(_._2).contains(selected)) 1 else -1
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
      data.map(x => (x._1, (x._2, labels(x._1))))
    })
    val (tp, tn, pos, neg) = lists.map(accuracy(_, weights)).reduce((x, y) => (x._1 + y._1, x._2 + y._2, x._3 + y._3, x._4 + y._4))
    ((tp + tn).toDouble / (pos + neg), tp.toDouble / pos, tn.toDouble / neg)
  }

  def accuracy(data: List[(Int, (Map[Int, Double], Int))], weights: Array[Double]): (Long, Long, Long, Long) = {
    val preds = data.map(p => {
      val (x, y) = p._2
      val pred = predict(x, weights)
      (y, pred == y)
    })
    val pos = preds.filter(p => p._1 == 1)

    val neg = preds.filter(p => p._1 == -1)

    val true_pos = pos.count(p => {
      p._2
    })

    val true_neg = neg.count(p => {
      p._2
    })

    (true_pos, true_neg, pos.size, neg.size)
  }
}
