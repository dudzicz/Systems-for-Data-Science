import java.lang.Double.max

class SVM {
  def gradient(x: List[Double], y: Double, w: List[Double]): List[Double] = {
    val a = y * scalar_product(w, x)
    if (a >= 1) x map (_ => 0.0) else scalar_mult(x, -y)
  }

  def loss(x: List[Double], y: Double, w: List[Double]): Double = {
    val a = 1 - y * scalar_product(w, x)
    max(a, 0)
  }

  def scalar_product(a: List[Double], b: List[Double]): Double = {
    a zip b map (x => x._1 * x._2) sum
  }


  def add(a: List[Double], b: List[Double]): List[Double] = {
    a zip b map (x => x._1 + x._2)
  }

  def scalar_mult(a: List[Double], b: Double): List[Double] = {
    a map (_ * b)
  }
}
