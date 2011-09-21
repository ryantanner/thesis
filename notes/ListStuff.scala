import scala.collection.JavaConverters._
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import edu.stanford.nlp.ling.TaggedWord
// List functions saved for future use
// Class definitions

class WTWord(w: String, t: String) {
// wrapped Scala version of stanford's TaggedWord for easier use
  val word: String = w
  val tag: String = t

  override def toString = w + "/" + t

  def this(w: TaggedWord) = this(w.word, w.tag) 

  def +(that: WTWord): WTWord =
    if (tag == that.tag)
      new WTWord(word + " " + that.word, tag)
    else
      new WTWord(word,tag) 
}

def group(list: List[WTWord]): List[WTWord] = list match {
// groups adjacent words of same tag, i.e. World/NNP War/NNP II/NNP so that their phrases are grouped
  case List() => List()
  case w1 :: w2 :: rest => if (w1.tag == w2.tag)
                              group((w1 + w2) :: rest)
                           else
                              w1 :: group(w2 :: rest)
  case w1 :: rest => w1 :: group(rest)
}

def initFile(path: String): List[List[WTWord]] = {
  // opens and tags file, returns collection of tagged sentences
  val tagger = new MaxentTagger("stanford-postagger-full-2010-05-26/models/left3words-wsj-0-18.tagger")
  val file = scala.io.Source.fromFile(path).mkString
  val reader = new java.io.StringReader(file)
  val sentences = MaxentTagger.tokenizeText(reader)
  val taggedSentences = tagger.process(sentences).asScala.toList map {
                        _.asScala.toList map {
                          {(word) => new WTWord(word)}
                        }
                      }
  taggedSentences
}
