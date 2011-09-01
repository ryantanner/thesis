class ParseTreeNode(par: ParseTreeNode, w: String, p: String, i: Int)   {

  val parent: ParseTreeNode = par
  var word:String = w
  var pos:String = p
  var childs:List[ParseTreeNode] = scala.collection.immutable.List()
  var index:Int = i

  override def toString = word + "(" + index + ")" + "/" + pos + " " + childs.length + " children"

  def this(p: String) = {
    this(null,"",p,-1)
  }

  def this() = {
    this(null,"","",-1)
  }

  def this(par: ParseTreeNode) = {
    this(par,"","",-1)
  }

  def :+(ptn: ParseTreeNode): ParseTreeNode = {
    // add child right
    childs = childs :+ ptn
    return this
  }

  def :+(par:ParseTreeNode, nw: String, np: String): ParseTreeNode = {
    childs = childs :+ new ParseTreeNode(par,nw,np,-1)
    return this
  }
  
  def :+(par:ParseTreeNode, nw: String, np: String, ni: Int): ParseTreeNode = {
    childs = childs :+ new ParseTreeNode(par,nw,np,ni)
    return this
  }

  def +:(ptn: ParseTreeNode) {
    childs = ptn +: childs
  }

  def +:(par:ParseTreeNode, nw: String, np: String) {
    childs = (new ParseTreeNode(par,nw,np,-1)) +: childs
  }

  def +:(par:ParseTreeNode, nw: String, np: String, ni: Int) {
    childs = (new ParseTreeNode(par,nw,np,ni)) +: childs
  }


}

val parse = loadNode \\ "parse" text

var parseList = parse.split(" ").toList


def pt(node: ParseTreeNode, elems:List[String]): ParseTreeNode = elems match {
	case List() => node
	case w :: rest => if ('(' == w.head) {
			val child = new ParseTreeNode(node)
			node :+ child
			child.pos = w slice (1,w.length)
			return pt(child,rest)
		}
		else {
			node.word = w replaceAll ("[)]","")
			var n = node
			for (i <- 1 to (w count { c => if (c == ')') true else false }))
				n = n.parent
			return pt(n,rest)
		}
}

// with index
def pt_i(node: ParseTreeNode, elems:List[String], i:Int): ParseTreeNode = elems match {
	case List() => node
	case w :: rest => if ('(' == w.head) {
			val child = new ParseTreeNode(node)
			node :+ child
			child.pos = w slice (1,w.length)
			return pt_i(child,rest,i)
		}
		else {
			node.word = w replaceAll ("[)]","")
			node.index = i
			var n = node
			for (i <- 1 to (w count { c => if (c == ')') true else false }))
				n = n.parent
			return pt_i(n,rest,i+1)
		}
}


// perhaps split on ( as well, use pattern matching
// parent not being used correctly?
// write tab-delemited tree visualization for debugging!

def printptn(ptn:ParseTreeNode, i:Int): Int = {
     var s = new StringBuilder
     for (t <- 1 to i)
     	s append "\t"
     s append ptn.toString
     println(s)
     ptn.childs map { printptn(_,i+1) }
     return i
}
