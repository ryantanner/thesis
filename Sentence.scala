package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:23 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Sentence {

  val tokens: List[Token]
  val dependencies: Map[String,List[thesis.Dependency]]
  val parseTree: ParseTreeNode // holds parent

  override def toString = tokens + "\n" + dependencies + "\n" + ParseTreeNode.print(parseTree)




}

object Sentence	{

/*
  val tokens: List[Token] = (List[Token]() /: tokNode) (_ :+ Token.fromXML(_))
  val dependencies: List[Dependency] = (List[Dependency]() /: depNode) (_ :+ Dependency.fromXML(_))
*/

  def fromXML(node: scala.xml.Node): Sentence =
    new Sentence {
      val tokens = (List[Token]() /: (node \\ "token")) (_ :+ Token.fromXML(_))
      val dependencies = 	(Map[String,List[Dependency]]() /: (node \\ "basic-dependencies" \\ "dep")) (
        (map:Map[String,List[Dependency]],dep:scala.xml.Node) => { map + (((dep \ "governor" \ "@idx").text) ->
            (map.getOrElse(((dep \ "governor" \ "@idx").text),List())
                      ++ List(Dependency.fromXML(dep,tokens))))})
      val parseTree = ParseTreeNode.parse(node \\ "parse" text, tokens, dependencies)
    }

}