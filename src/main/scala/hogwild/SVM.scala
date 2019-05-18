package hogwild

import java.lang.Double.max

/**
  * An object giving static methods to help with the SVM computations.
  */
object SVM {


  /**
    * Computes the loss for the given data-point.
    *
    * @param data   the data-point
    * @param w      the model's weight vector
    * @param lambda the regularization coefficient
    * @return the loss for the given data-point
    */
  def loss(data: (Int, (Map[Int, Double], Int)), w: Array[Double], lambda: Double): Double = {
    val (_, (x, y)) = data
    loss(x, y, w, lambda)
  }

  /**
    * Computes the loss for the given data-point.
    *
    * @param x      the input vector, in sparse representation
    * @param y      the target label (1 or -1)
    * @param w      the model's weight vector
    * @param lambda the regularization coefficient
    * @return the loss for the given data-point
    */
  def loss(x: Map[Int, Double], y: Int, w: Array[Double], lambda: Double): Double = {
    val e = x.keys
    val xe = x.values
    val we = select(e, w)
    val a = 1 - y * scalarProduct(we, xe)
    max(a, 0) + lambda * we.map(i => i * i).sum
  }

  /**
    * Computes the gradient for the given data-point.
    *
    * @param x      the input vector
    * @param y      the target label (1 or -1)
    * @param w      the model's weight vector
    * @param lambda the regularization coefficient
    * @return the gradient for the given data-point
    */
  def gradient(x: Iterable[Double], y: Int, w: Iterable[Double], lambda: Double): Iterable[Double] = {
    val a = y * scalarProduct(w, x)
    val regularizer = scalarMultiplication(w, 2 * lambda)
    if (a >= 1) regularizer else add(regularizer, scalarMultiplication(x, -y))
  }

  /**
    * Computes the sparse gradient for the given data-point.
    *
    * @param data   the data-point
    * @param w      the model's weight vector
    * @param lambda the regularization coefficient
    * @return the sparse gradient for the given data-point
    */
  def sparseGradient(data: (Int, (Map[Int, Double], Int)), w: Array[Double], lambda: Double): Map[Int, Double] = {
    val (_, (x, y)) = data
    sparseGradient(x, y, w, lambda)
  }

  /**
    * Computes the sparse gradient for the given data-point.
    *
    * @param x      the input vector, in sparse representation
    * @param y      the target label (1 or -1)
    * @param w      the model's weight vector
    * @param lambda the regularization coefficient
    * @return the sparse gradient for the given data-point
    */
  def sparseGradient(x: Map[Int, Double], y: Int, w: Array[Double], lambda: Double): Map[Int, Double] = {
    val e = x.keys
    val xe = x.values
    val we = select(e, w)
    val g = gradient(xe, y, we, lambda)
    e.zip(g).toMap
  }

  def expandSparseGradient(gradient: Map[Int, Double]): Map[Int, (Double, Int)] = {
    gradient.map { case (e, g) => e -> (g, 1) }
  }


  /**
    * Merges two sparse gradient together.
    *
    * @param m1 the first sparse gradient
    * @param m2 the second sparse gradient
    * @return the combined sparse gradient
    */
  def mergeSparseGradient(m1: Map[Int, (Double, Int)], m2: Map[Int, (Double, Int)]): Map[Int, (Double, Int)] = {
    m2 ++ m1.map {
      case (k, v) => k -> (v._1 + m2.getOrElse(k, (0.0, 0))._1, v._2 + m2.getOrElse(k, (0.0, 0))._2)
    }
  }

  /**
    * Updates the given weight vector with the given sparse gradient and learning rate.
    *
    * @param weight   the weight vector
    * @param gradient the sparse gradient
    * @param gamma    the learning rate
    * @return the weight vector
    */
  def updateWeight(weight: Array[Double], gradient: Map[Int, (Double, Int)], gamma: Double): Array[Double] = {
    for ((e, (g, d)) <- gradient) {
      weight(e) -= gamma * (g / d)
    }
    weight
  }

  /**
    * Computes the model prediction for the given data-point.
    *
    * @param x the input vector, in sparse representation
    * @param w the model's weight vector
    * @return the predicted label (1 or -1)
    */
  def predict(x: Map[Int, Double], w: Array[Double]): Int = {
    val e = x.keys
    val xe = x.values
    val we = select(e, w)
    if (scalarProduct(xe, we) >= 0) 1 else -1
  }

  /**
    * Helper function to select the given indices in an Array.
    *
    * @param indices the indices to select
    * @param array   the array to select from
    * @return the array's values for the given indices
    */
  private def select(indices: Iterable[Int], array: Array[Double]): Iterable[Double] = {
    indices.map(array)
  }

  /**
    * Helper function to compute the scalar product between two vectors.
    *
    * @param v1 the first vector
    * @param v2 the second vector
    * @return the scalar product between the two given vectors
    */
  private def scalarProduct(v1: Iterable[Double], v2: Iterable[Double]): Double = {
    v1.zip(v2).map { case (a, b) => a * b }.sum
  }

  /**
    * Helper function to multiply the given vector by a scalar.
    *
    * @param v the vector
    * @param c the scalar
    * @return the vector scaled by the given scalar
    */
  private def scalarMultiplication(v: Iterable[Double], c: Double): Iterable[Double] = {
    v.map(_ * c)
  }

  /**
    * Helper function to compute the element-wise addition between two vectors.
    *
    * @param v1 the first vector
    * @param v2 the second vector
    * @return the element-wise addition between the two given vectors
    */
  private def add(v1: Iterable[Double], v2: Iterable[Double]): Iterable[Double] = {
    v1.zip(v2).map { case (a, b) => a + b }
  }

}
