package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Dependency	{

  val gov: ParseTreeNode
  val dep: ParseTreeNode
  val relType: String // dependency relation/type
  val relFunc: Relations.dep

  override def toString = "(" + gov + " > " + dep + " / " + relType + ")"

}

object Dependency	{
  def fromXML(node: scala.xml.Node, nodes: Map[Int,ParseTreeNode]): Dependency =
    new Dependency	{
      val gov = nodes((node \ "governor" \ "@idx").text.toInt)
      val dep = nodes((node \ "dependent" \ "@idx").text.toInt)
      val relType = (node \ "@type") text
      val relFunc = Relations.handler(relType)
    }
}