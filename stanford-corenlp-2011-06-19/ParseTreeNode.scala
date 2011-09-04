abstract class Sentence {

	val tokens: List[Token]
	val dependencies: List[Dependency]
	val parseTree: ParseTreeNode // holds parent
	
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

	abstract class Dependency	{
	
		val gov: Token 
		val dep: Token 
		val rel: String // dependency relation/type
		
		override def toString = "(" + gov + " > " + dep + " / " + rel + ")"
	
	}

	object Dependency	{
		
		def fromXML(node: scala.xml.Node): Dependency =
			new Dependency	{
				val gov = tokens((node \ "governor" \ "@idx").text.toInt - 1)
				val dep = tokens((node \ "dependent" \ "@idx").text.toInt - 1)
				val rel = (node \ "@type") text
			}
	
	}

	class ParseTreeNode(par: ParseTreeNode, w: Token, p: String, i: Int)   {
	
	  val parent: ParseTreeNode = par
	  //var word:String = w
	  var pos:String = p
	  var word:Token = w
	  var childs:List[ParseTreeNode] = scala.collection.immutable.List()
	  var index:Int = i
	  var relations: List[Dependency] = List[Dependency]()
	
	  override def toString = word + "(" + index + ")" + "/" + pos + " " + childs.length + " children"
	
	  def this(p: String) = {
		this(null,null,p,-1)
	  }
	
	  def this() = {
		this(null,null,"",-1)
	  }
	
	  def this(par: ParseTreeNode) = {
		this(par,null,"",-1)
	  }
	
	  def :+(ptn: ParseTreeNode): ParseTreeNode = {
		// add child right
		childs = childs :+ ptn
		return this
	  }
	
	  def :+(par:ParseTreeNode, nw: Token, np: String): ParseTreeNode = {
		childs = childs :+ new ParseTreeNode(par,nw,np,-1)
		return this
	  }
	  
	  def :+(par:ParseTreeNode, nw: Token, np: String, ni: Int): ParseTreeNode = {
		childs = childs :+ new ParseTreeNode(par,nw,np,ni)
		return this
	  }
	
	  def +:(ptn: ParseTreeNode) {
		childs = ptn +: childs
	  }
	
	  def +:(par:ParseTreeNode, nw: Token, np: String) {
		childs = (new ParseTreeNode(par,nw,np,-1)) +: childs
	  }
	
	  def +:(par:ParseTreeNode, nw: Token, np: String, ni: Int) {
		childs = (new ParseTreeNode(par,nw,np,ni)) +: childs
	  }
	
	
	}
	
	object ParseTreeNode	{
		
		def parse(parseString: String): ParseTreeNode = {
			return pt_i(new ParseTreeNode, parseString.split(" ").toList,0)	
		}
		
		// with index
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
	


}

object Sentence	{

/*
	val tokens: List[Token] = (List[Token]() /: tokNode) (_ :+ Token.fromXML(_))
	val dependencies: List[Dependency] = (List[Dependency]() /: depNode) (_ :+ Dependency.fromXML(_))
*/
	
	def fromXML(node: scala.xml.Node): Sentence =
		new Sentence {
			val tokens = (List[Token]() /: (node \\ "token")) (_ :+ Token.fromXML(_))
			val dependencies = (List[Dependency]() /: (node \\ "basic-dependencies" \\ "dep")) (_ :+ Dependency.fromXML(_))
			val parseTree = ParseTreeNode.parse(node \\ "parse" text)
		}

}



