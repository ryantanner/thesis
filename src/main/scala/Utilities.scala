package thesis

object Utilities {

  def multiFlatten[T](list: List[T]): List[T] = list flatMap {
    case ls: List[T] => multiFlatten(ls)
    case e => List[T](e)
  }

}
