package distributed

import java.lang.Double.max

import org.apache.spark.rdd.RDD

object SVM {
  def loss(x: Map[Int, Double], y: Int, w: Array[Double], lambda: Double): Double = {
    val xe = x.values.toList
    val we = select(x, w)
    val a = 1 - y * scalar_product(we, xe)
    max(a, 0) + lambda * we.map(i => i * i).sum
  }

  def svm(x: Map[Int, Double], y: Int, w: Array[Double], lambda: Double): Map[Int, Double] = {
    val xe = x.values.toList
    val we = select(x, w)
    val g = gradient(xe, y, we, lambda)
    x.keys.zip(g).toMap
  }

  def gradient(x: List[Double], y: Int, w: List[Double], lambda: Double): List[Double] = {
    val a = y * scalar_product(w, x)
    add(scalar_mult(w, 2 * lambda), if (a >= 1) x map (_ => 0.0) else scalar_mult(x, -y))
  }

  def scalar_product(a: Iterable[Double], b: List[Double]): Double = {
    a zip b map (x => x._1 * x._2) sum
  }

  def scalar_mult(a: List[Double], b: Double): List[Double] = {
    a map (_ * b)
  }

  def add(a: List[Double], b: List[Double]): List[Double] = {
    a zip b map (x => x._1 + x._2)
  }

  def select(x: Map[Int, Double], w: Array[Double]): List[Double] = {
    x.keys.toList map w
  }

  def add(a: Array[Double], b: Array[Double]): Array[Double] = {
    a zip b map (x => x._1 + x._2)
  }

  def update_weight(w: Array[Double], grad: Map[Int, Double], gamma: Double): Array[Double] = {
    for ((a, b) <- grad) {
      w(a) -= gamma * b
    }
    w
  }

  //Optimize helper function to merge gradients in a faster manner
  def merge(m1: Map[Int, (Double, Int)], m2: Map[Int, (Double, Int)]): Map[Int, (Double, Int)] = {
    m2 ++ m1.map {
      case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0, 0))._1, v._2 + m2.getOrElse(k, (0.0, 0))._2)
    }
  }

  def accuracy(data: RDD[(Int, (Map[Int, Double], Int))], weights: Array[Double]): (Long, Long, Long, Long) = {
    val preds = data.map(p => {
      val (x, y) = p._2
      val pred = predict(x, weights)
      (y, pred == y)
    })
    preds.cache()
    val pos = preds.filter(p => p._1 == 1)
    pos.cache()

    val neg = preds.filter(p => p._1 == -1)
    neg.cache()

    val true_pos = pos.filter(p => {
      p._2
    }).count()

    val true_neg = neg.filter(p => {
      p._2
    }).count()

    (true_pos, true_neg, pos.count(), neg.count())
  }

  def predict(x: Map[Int, Double], w: Array[Double]): Int = {
    val xe = x.values.toList
    val we = select(x, w)
    if (scalar_product(xe, we) >= 0) 1 else -1
  }
}
