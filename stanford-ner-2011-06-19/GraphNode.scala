import scala.collection.immutable.List
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.util.CoreMap

class Node(n: String)  {
  
  val name: String = n 
  var entityType = "" 

  

  def setEntityType(et: String)   {
    entityType = et
  }

}


class EntityWord(w: String, t: String) {
  val word = w
  val entityType = t

  override def toString = w + "/" + t

  def this(that: CoreMap) = this(that.get[String,TextAnnotation](classOf[TextAnnotation]), that.get[String,AnswerAnnotation](classOf[AnswerAnnotation]))
  
    def this(toks: List[scala.xml.Node]) = {
        this(toks \ "word" map { w => w.text } reduceRight ((a,b) => a + " " + b),toks(0) \ "NER" text)

    }

  /*def +(that: CoreMap): EntityWord =
    entityType match {
      case that.get[String,AnswerAnnotation](classOf[AnswerAnnotation]) => new EntityWord(word + " " + that.get[String,TextAnnotation](classOf[TextAnnotation]), entityType)
      case _ => new EntityWord(word,entityType)
    }*/

  def +(that: CoreMap): EntityWord = 
    if (entityType == that.get[String,AnswerAnnotation](classOf[AnswerAnnotation]) & entityType != "O")
      new EntityWord(word + " " + that.get[String,TextAnnotation](classOf[TextAnnotation]), entityType)
    else
      new EntityWord(word,entityType)


  def +(that: EntityWord): EntityWord =
    if ((entityType == that.entityType) & entityType != "0")
      new EntityWord(word + " " + that.word, entityType)
    else
      new EntityWord(word,entityType)

}

def group(list: List[EntityWord]): List[EntityWord] = list match {
  case List() => List()
  case w1 :: w2 :: rest => if ((w1.entityType == w2.entityType) & (w1.entityType != "O"))
                              group((w1 + w2) :: rest)
                           else
                              w1 :: group(w2 :: rest)
  case w1 :: rest => w1 :: group(rest)
}

def filterList(nodes: NodeSeq, indices: List[Int]): List[Int] = {     
    if (!nodes.isEmpty)     
        return filterList(nodes tail, indices filterNot { i => i == ((nodes head) \ "@idx").text.toInt } )
    else
        return indices
}

class Connection(w1: EntityWord, w2: String, t: String) {
    val word:EntityWord = w1
    val connect:String = w2
    val verb: String = t
    
    override def toString = w1.word + "-" + w2 + "/" + t
}



