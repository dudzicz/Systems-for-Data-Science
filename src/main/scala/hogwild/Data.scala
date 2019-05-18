package hogwild

import java.lang.Integer.max

import hogwild.Parameters.SELECT_LABEL
import hogwild.SVM.predict
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.io.Source

/**
  * An object giving static methods to help with data loading and accuracy computations.
  */
object Data {

  /**
    * Loads the train data as an RDD.
    *
    * @param sc   the SparkContext to use to load the data
    * @param path the path of the directory containing the data
    * @return the data loaded as a sparse format, and the number of dimensions in the data
    */
  def loadDataRDD(sc: SparkContext, path: String): (RDD[(Int, (Map[Int, Double], Int))], Int) = {
    val dataLines = sc.textFile("file://" + path + "lyrl2004_vectors_train.dat")
    val topics = sc.textFile("file://" + path + "rcv1-v2.topics.qrels")
    val labels = topics.map(parseTopics).groupByKey().mapValues(topicsToLabel(_, SELECT_LABEL))
    val data = dataLines.map(parseData)
    val dimensions = data.map { case (_, x) => x.keys.max }.reduce(max) + 1
    val fullData = data.join(labels).cache()
    (fullData, dimensions)
  }

  /**
    * Loads the train data as a List.
    *
    * @param path the path of the directory containing the data
    * @return the data loaded as a sparse format, and the number of dimensions in the data
    */
  def loadData(path: String): (List[(Int, (Map[Int, Double], Int))], Int) = {
    val datalines = Source.fromFile(path + "lyrl2004_vectors_train.dat").getLines().toList
    val topics = Source.fromFile(path + "rcv1-v2.topics.qrels").getLines().toList

    val labels = topics.map(parseTopics).groupBy(_._1).mapValues(cat => topicsToLabel(cat.map(_._2), SELECT_LABEL))
    val data = datalines.map(parseData)
    val fullData = data.map { case (i, x) => (i, (x, labels(i))) }
    val dimensions = data.map(x => x._2.keys.max).reduce(max) + 1
    (fullData, dimensions)
  }

  /**
    * Helper function used to parse the topic information.
    *
    * @param line the line to parse
    * @return the parsed topic
    */
  private def parseTopics(line: String): (Int, String) = {
    val split = line.split(" ")
    split(1).toInt -> split(0)
  }

  /**
    * Helper function used to transform the list of topics to a label.
    *
    * @param topics   the list of topics
    * @param selected the selected category
    * @return 1 if the selected category is part of the topics, -1 otherwise
    */
  private def topicsToLabel(topics: Iterable[String], selected: String): Int = {
    if (topics.toList.contains(selected)) 1 else -1
  }

  /**
    * Helper function used to parse the sparse data.
    *
    * @param line the line to parse
    * @return the data-point, in sparse representation
    */
  private def parseData(line: String): (Int, Map[Int, Double]) = {
    val split = line.split("  ")
    val index = split(0).toInt
    val coords = split(1).split(" ")
    val map = coords.map(j => {
      val p = j.split(":")
      p(0).toInt -> p(1).toDouble
    }).toMap + (0 -> 1.0)
    (index, map)
  }

  /**
    * Computes the test accuracy for the given model.
    *
    * @param sc      the SparkContext to use to load the data
    * @param path    the path of the directory containing the data
    * @param weights the model's weight vector
    * @return the total accuracy, positive class accuracy and negative class accuracy
    */
  def testAccuracy(sc: SparkContext, path: String, weights: Array[Double]): (Double, Double, Double) = {
    val testIndex = 0 until 4
    val topics = sc.textFile("file://" + path + "rcv1-v2.topics.qrels")
    val labels = topics.map(parseTopics).groupBy(_._1).mapValues(cat => topicsToLabel(cat.map(_._2), SELECT_LABEL))

    val rdds = testIndex.map(i => {
      val datalines = sc.textFile("file://" + path + "lyrl2004_vectors_test_pt" + i + ".dat")
      val data = datalines.map(parseData)
      data.join(labels)
    })
    val (tp, tn, pos, neg) = rdds.map(accuracy(_, weights)).reduce(mergeTuples)
    acc(tp, tn, pos, neg)
  }

  /**
    * Computes the test accuracy for the given model.
    *
    * @param path    the path of the directory containing the data
    * @param weights the model's weight vector
    * @return the total accuracy, positive class accuracy and negative class accuracy
    */
  def testAccuracy(path: String, weights: Array[Double]): (Double, Double, Double) = {
    val testIndex = 0 until 4
    val topics = Source.fromFile(path + "rcv1-v2.topics.qrels").getLines().toList
    val labels = topics.map(parseTopics).groupBy(_._1).mapValues(cat => topicsToLabel(cat.map(_._2), SELECT_LABEL))

    val lists = testIndex.map(i => {
      val datalines = Source.fromFile(path + "lyrl2004_vectors_test_pt" + i + ".dat").getLines().toList
      val data = datalines.map(parseData)
      data.map(x => (x._1, (x._2, labels(x._1))))
    })
    val (tp, tn, pos, neg) = lists.map(accuracy(_, weights)).reduce(mergeTuples)
    acc(tp, tn, pos, neg)
  }

  /**
    * Computes the model's accuracy on the given data.
    *
    * @param data    the data to compute the accuracy on.
    * @param weights the model's weight vector
    * @return the number of true positive, true negative, number of positive, and number of negative
    */
  def accuracy(data: List[(Int, (Map[Int, Double], Int))], weights: Array[Double]): (Long, Long, Long, Long) = {
    val predictions = data.map { case (_, (x, y)) => (y, predict(x, weights) == y) }
    val pos = predictions.filter { case (y, _) => y == 1 }
    val neg = predictions.filter { case (y, _) => y == -1 }
    val true_pos = pos.count { case (_, t) => t }
    val true_neg = neg.count { case (_, t) => t }
    (true_pos, true_neg, pos.size, neg.size)
  }

  /**
    * Computes the model's accuracy on the given data.
    *
    * @param data    the data to compute the accuracy on.
    * @param weights the model's weight vector
    * @return the number of true positive, true negative, number of positive, and number of negative
    */
  def accuracy(data: RDD[(Int, (Map[Int, Double], Int))], weights: Array[Double]): (Long, Long, Long, Long) = {
    val predictions = data.map { case (_, (x, y)) => (y, predict(x, weights) == y) }.cache()
    val pos = predictions.filter(p => p._1 == 1).cache()
    val neg = predictions.filter(p => p._1 == -1).cache()
    val true_pos = pos.filter { case (_, t) => t }.count()
    val true_neg = neg.filter { case (_, t) => t }.count()
    (true_pos, true_neg, pos.count(), neg.count())
  }

  /**
    * Helper functions to add Long 4-tuple.
    *
    * @param t1 the first tuple
    * @param t2 the second tuple
    * @return the element-wise sum of both tuple
    */
  private def mergeTuples(t1: (Long, Long, Long, Long), t2: (Long, Long, Long, Long)): (Long, Long, Long, Long) = {
    (t1._1 + t2._1, t1._2 + t2._2, t1._3 + t2._3, t1._4 + t2._4)
  }

  /**
    * Helper functions helping compute the accuracies from the number of true positives/negatives.
    *
    * @param tp  the number of true positives
    * @param tn  the number of true negatives
    * @param pos the number of positives
    * @param neg the number of negatives
    * @return the total accuracy, positive class accuracy and negative class accuracy
    */
  private def acc(tp: Long, tn: Long, pos: Long, neg: Long): (Double, Double, Double) = {
    ((tp + tn).toDouble / (pos + neg), tp.toDouble / pos, tn.toDouble / neg)
  }
}
