package thesis

import thesis.db._

object Utilities {

  def multiFlatten[T](list: List[T]): List[T] = list flatMap {
    case ls: List[T] => multiFlatten(ls)
    case e => List[T](e)
  }

}

import scala.concurrent._
import java.io.File

class LockN(n: Int) extends Lock {
    var currentN = n

    override def acquire = synchronized {
        while(currentN <= 0) wait()
        currentN -= 1
    }

    override def release = synchronized {
        currentN += 1
        notify()
    }
}

class DocRunner(f: File) {
    def run = {
      val doc = Document.fromFile(f)
      ORMTests.insertAllFromDocument(doc)
    }
}
