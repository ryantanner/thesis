package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 5:27 PM
 * To change this template use File | Settings | File Templates.
 */

class ParseTreeNode(par: ParseTreeNode, w: Token, p: String)   {

  val parent: ParseTreeNode = par
  //var word:String = w
  var pos:String = p
  var word:Token = w
  var childs:List[ParseTreeNode] = scala.collection.immutable.List()
  //var index:Int = i
  var dependents: List[Dependency] = List[Dependency]()


  override def toString = if (word == null) "(" + pos + " " + childs.length + " children"
                else word + " (" + word.id + ") / " + pos  + " " + dependents.length + " dependents"

  def this(p: String) = {
  this(null,null,p)
  }

  def this() = {
  this(null,null,"")
  }

  def this(par: ParseTreeNode) = {
  this(par,null,"")
  }

  def :+(ptn: ParseTreeNode): ParseTreeNode = {
  // add child right
  childs = childs :+ ptn
  return this
  }

  def :+(par:ParseTreeNode, nw: Token, np: String): ParseTreeNode = {
  childs = childs :+ new ParseTreeNode(par,nw,np)
  return this
  }

  def +:(ptn: ParseTreeNode) {
  childs = ptn +: childs
  }

  def +:(par:ParseTreeNode, nw: Token, np: String) {
  childs = (new ParseTreeNode(par,nw,np)) +: childs
  }

}

object ParseTreeNode	{

  def parse(parseString: String, tokens: List[Token], dependencies:Map[String,List[thesis.Dependency]]): ParseTreeNode = {
    return parser(new ParseTreeNode, parseString.split(" ").toList,tokens, dependencies)
  }

/*
 with index
		private def pt_i(node: ParseTreeNode, elems:List[String], i:Int): ParseTreeNode = elems match {
			case List() => node
			case w :: rest => if ('(' == w.head) {
					val child = new ParseTreeNode(node)
					node :+ child
					child.pos = w slice (1,w.length)
					return pt_i(child,rest,i)
				}
				else {
					node.word = tokens(i)
					node.index = i
					var n = node
					for (i <- 1 to (w count { c => if (c == ')') true else false }))
						n = n.parent
					return pt_i(n,rest,i+1)
				}
		}
*/


  def parser(node: ParseTreeNode, elems:List[String], tokens:List[Token], dependencies:Map[String,List[thesis.Dependency]]): ParseTreeNode = elems match {
    case List() => node
    case w :: rest => if ('(' == w.head) {
        val child = new ParseTreeNode(node)
        node :+ child
        child.pos = w slice (1,w.length)
        return parser(child,rest,tokens,dependencies)
      }
      else {
        node.word = tokens.head
        node.dependents = dependencies.getOrElse(tokens.head.id,List())
        var n = node
        for (i <- 1 to (w count { c => if (c == ')') true else false }))
          n = n.parent
        return parser(n,rest,tokens tail, dependencies)
      }
  }


  private def print_i(ptn:ParseTreeNode, i:Int): Int = {
     var s = new StringBuilder
     for (t <- 1 to i)
      s append "\t"
     s append ptn.toString
     println(s)
     ptn.childs map { print_i(_,i+1) }
     return i
  }

  def print(ptn: ParseTreeNode) = {
    print_i(ptn,1)
  }


}


