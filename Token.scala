package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:24 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Token	{

  val id: String
  val word: String
  val lemma: String
  val begin: Int
  val end: Int
  val pos: String
  val ner: String

  override def toString = word

  def toXML =
    <token id={id}>
      <word>{word}</word>
      <lemma>{lemma}</lemma>
      <CharacterOffsetBegin>{begin}</CharacterOffsetBegin>
      <CharacterOffsetEnd>{end}</CharacterOffsetEnd>
      <POS>{pos}</POS>
      <NER>{ner}</NER>
    </token>

}

object Token	{

  def fromXML(node: scala.xml.Node): Token =
    new Token {
      val id = (node \ "@id").text
      val word = (node \ "word").text
      val lemma = (node \ "lemma").text
      val begin = (node \ "CharacterOffsetBegin").text.toInt
      val end = (node \ "CharacterOffsetEnd").text.toInt
      val pos = (node \ "pos").text
      val ner = (node \ "ner").text
    }

}
