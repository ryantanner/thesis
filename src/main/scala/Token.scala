package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:24 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Token extends Ordered[Token]	{

	val sent: Int
  val id: Int
  val word: String
  val lemma: String
  val begin: Int
  val end: Int
  val pos: String
  val ner: String

  override def toString = word

  def toXML =
    <token id={id.toString}>
      <word>{word}</word>
      <lemma>{lemma}</lemma>
      <CharacterOffsetBegin>{begin}</CharacterOffsetBegin>
      <CharacterOffsetEnd>{end}</CharacterOffsetEnd>
      <POS>{pos}</POS>
      <NER>{ner}</NER>
    </token>

	def compare(that: Token) = this.id - that.id

}

object Token	{
	
	var curSentence:Int = -1

  def fromXML(node: scala.xml.Node): Token =
    new Token {
	  val sent = curSentence
      val id = (node \ "@id").text.toInt
      val word = (node \ "word").text
      val lemma = (node \ "lemma").text
      val begin = (node \ "CharacterOffsetBegin").text.toInt
      val end = (node \ "CharacterOffsetEnd").text.toInt
      val pos = (node \ "POS").text
      val ner = (node \ "NER").text
    }

}

class EmptyToken extends Token{
  val sent = -1
  val id: Int = 0
  val word: String = ""
  val lemma: String = ""
  val begin: Int = 0
  val end: Int = 0
  val pos: String = ""
  val ner: String = "O"

}