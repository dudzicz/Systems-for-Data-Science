package svm

import java.lang.Double.max

object SVM {
  def loss(x: List[Double], y: Int, w: List[Double]): Double = {
    val a = 1 - y * scalar_product(w, x)
    max(a, 0)
  }

  def scalar_product(a: List[Double], b: List[Double]): Double = {
    a zip b map (x => x._1 * x._2) sum
  }

  def svm(x: Map[Int, Double], y: Int, w: List[Double]): Map[Int, Double] = {
    val xe = x.values.toList
    val we = select(x, w)
    val g = gradient(xe, y, we)
    x.keys.zip(g).toMap
  }

  def gradient(x: List[Double], y: Int, w: List[Double]): List[Double] = {
    val a = y * scalar_product(w, x)
    if (a >= 1) x map (_ => 0.0) else scalar_mult(x, -y)
  }

  def scalar_mult(a: List[Double], b: Double): List[Double] = {
    a map (_ * b)
  }

  def select(x: Map[Int, Double], w: List[Double]): List[Double] = {
    x.keys.toList map w
  }

  def add(a: List[Double], b: List[Double]): List[Double] = {
    a zip b map (x => x._1 + x._2)
  }
}