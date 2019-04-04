package svm

import java.lang.Double.max

object SVM {
  def loss(x: Map[Int, Double], y: Int, w: Array[Double], lambda: Double): Double = {
    val xe = x.values.toList
    val we = select(x, w)
    val a = 1 - y * scalar_product(we, xe)
    max(a, 0) + lambda * we.map(i => i * i).sum
  }

  def scalar_product(a: Iterable[Double], b: List[Double]): Double = {
    a zip b map (x => x._1 * x._2) sum
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

  def scalar_mult(a: List[Double], b: Double): List[Double] = {
    a map (_ * b)
  }

  def select(x: Map[Int, Double], w: Array[Double]): List[Double] = {
    x.keys.toList map w
  }

  def add(a: List[Double], b: List[Double]): List[Double] = {
    a zip b map (x => x._1 + x._2)
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
}