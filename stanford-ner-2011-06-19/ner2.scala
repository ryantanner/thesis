/*scala> for ((sentence:List[_ <: CoreMap]) <- out) {
     | for ((word:CoreMap) <- sentence) {
     | val w = word match {
     | case w2: CoreLabel => w2
     | case _ => throw new ClassCastException
     | }
     | println(w.word + '/' + w.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]) + ' ')
     | }
     | }
World/O*/

import edu.stanford.nlp.ie.crf._
import edu.stanford.nlp.ie.AbstractSequenceClassifier
import edu.stanford.nlp.io.IOUtils
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation
import java.util.List
import java.io.IOException
import scala.collection.JavaConversions._

val serializedClassifier = "classifiers/all.3class.distsim.crf.ser.gz"

val classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier)

val ww2strings = IOUtils.slurpFile("ww2sample.txt")
val out = classifier.classify(ww2strings)

val tagged = for ((sentence:List[_ <% CoreLabel]) <- out) yield {
  for ((word: CoreLabel) <- sentence) yield {
    val w = word match {
      case w2: CoreLabel => w2
      case _ => throw new ClassCastException
    }
//    if (w.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]) != 'O')
//      yield w2
    w.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]) match {
      case "O" => {}
     case _ => w 
   }
  }
}

//println(tagged.filterNot(_.isEmpty))

val names = tagged map { s => 
  s filter { w =>
    w match {
      case w2: CoreMap => true
      case _ => false
    }
  }
}

//names foreach { _.foreach(println(_)) }
//import scala.collection.mutable.ListBuffer
//val nodes = new ListBuffer[Node]
/*for (s <- tagged) {
  for (w <- s) {
    val word = w match {
      case w2: CoreLabel => w2
      case _ => throw new ClassCastException
    }
    val n = new Node(word.word)
    n.setEntityType(word.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]))
    nodes += n
  }
}*/

def group(list: List[_ <: CoreMap]): List[_ <: CoreMap] = list match {
  case List() => List()
  case w1 :: w2 :: rest => if (w1.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]) == w2.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]))
                              group((w1 :: w2) :: rest)
                            else
                              w1 :: group(w2 :: rest)
  case w1 :: rest => w1 :: group(rest)
}

class WTWord(w: String, t: String) {
// wrapped Scala version of stanford's TaggedWord for easier use
  val word: String = w
  val tag: String = t

  override def toString = w + "/" + t

  def this(w: TaggedWord) = this(w.word, w.tag)

  def +(that: WTWord): WTWord =    if (tag == that.tag)      new WTWord(word + " " + that.word, tag)
    else
      new WTWord(word,tag) 
}

def group(list: List[_ <: CoreMap]): List[_ <: CoreMap] = list match {
// groups adjacent words of same tag, i.e. World/NNP War/NNP II/NNP so that their phrases are grouped
  case List() => List()
  case w1 :: w2 :: rest => if (w1.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation])== w2.get[String,CoreAnnotations.AnswerAnnotation](classOf[edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation]))
                              group((w1 + w2) :: rest)
                           else   
                              w1 :: group(w2 :: rest)
  case w1 :: rest => w1 :: group(rest)
}

