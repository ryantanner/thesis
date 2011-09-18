package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Dependency	{

  val gov: Token
  val dep: Token
  val relType: String // dependency relation/type
  val relFunc: Relations.dep

  override def toString = "(" + gov + " > " + dep + " / " + relType + ")"

}

object Dependency	{
  def fromXML(node: scala.xml.Node, tokens: List[Token]): Dependency =
    new Dependency	{
      val gov = tokens((node \ "governor" \ "@idx").text.toInt - 1)
      val dep = tokens((node \ "dependent" \ "@idx").text.toInt - 1)
      val relType = (node \ "@type") text
      val relFunc = Relations.handler(relType).apply(gov,dep)
    }
}