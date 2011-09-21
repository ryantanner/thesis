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
    val dependencies: Map[Int,List[Dependency]]      // (gov, list of deps)
    val parseTree: ParseTreeNode // holds parent
	val nodes: Map[Int,ParseTreeNode] // nodes and their index
	var entities: Map[Entity,List[Property]]

    override def toString = tokens + "\n" + dependencies + "\n" + ParseTreeNode.print(parseTree)

}

object Sentence	{

/*
  val tokens: List[Token] = (List[Token]() /: tokNode) (_ :+ Token.fromXML(_))
  val dependencies: List[Dependency] = (List[Dependency]() /: depNode) (_ :+ Dependency.fromXML(_))
*/

	var sent:Sentence = new EmptySentence()

	def fromXML(node: scala.xml.Node): Sentence =  {
		sent = new Sentence {
			val tokens = (List[Token](new EmptyToken) /: (node \\ "token")) (_ :+ Token.fromXML(_))
			val (parseTree:ParseTreeNode, nodes:Map[Int,ParseTreeNode]) = ParseTreeNode.parse(node \\ "parse" text, tokens, dependencies)
			val dependencies = 	(Map[Int,List[Dependency]]() /: (node \\ "basic-dependencies" \\ "dep")) (
			(map:Map[Int,List[Dependency]],dep:scala.xml.Node) => { map + (((dep \ "governor" \ "@idx").text.toInt)
					->
				(map.getOrElse(((dep \ "governor" \ "@idx").text.toInt),List())
						  ++ List(Dependency.fromXML(dep,nodes))))})
			// build parsetree first, then dependencies.  after built, go back and add indices to deps
			nodes foreach { node => node._2.dependents = dependencies.getOrElse(node._2.word.id,List[Dependency]()) map { _.dep.word.id } }
			var entities = Entity.entityMap(tokens)
		}
		return sent
	}
}

class EmptySentence extends Sentence    {
	import scala.collection.immutable.HashMap

	val tokens: List[Token] = List[Token]()
	val dependencies: Map[Int,List[Dependency]] = new HashMap[Int,List[Dependency]]
    val parseTree: ParseTreeNode = new ParseTreeNode
	val nodes: Map[Int,ParseTreeNode] = new HashMap()

}