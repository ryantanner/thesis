import scala.xml._

abstract class Sentence {

	val tokens: List[Token]
	val dependencies: Map[String,List[Dependency]]
	val parseTree: ParseTreeNode // holds parent
	
	override def toString = tokens + "\n" + dependencies + "\n" + ParseTreeNode.print(parseTree)
	
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
		val relType: String // dependency relation/type
		val relFunc: (ParseTreeNode,ParseTreeNode) => ParseTreeNode
		
		override def toString = "(" + gov + " > " + dep + " / " + relType + ")"
	
	}

	object Dependency	{
	
		
		
		def fromXML(node: scala.xml.Node): Dependency =
			new Dependency	{
				val gov = tokens((node \ "governor" \ "@idx").text.toInt - 1)
				val dep = tokens((node \ "dependent" \ "@idx").text.toInt - 1)
				val relType = (node \ "@type") text
				val relFunc = Relations.handler(relType) 
			}
	
	}

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
		
		def parse(parseString: String): ParseTreeNode = {
			return parser(new ParseTreeNode, parseString.split(" ").toList,tokens)	
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

		
		def parser(node: ParseTreeNode, elems:List[String], tokens:List[Token]): ParseTreeNode = elems match {
			case List() => node
			case w :: rest => if ('(' == w.head) {
					val child = new ParseTreeNode(node)
					node :+ child
					child.pos = w slice (1,w.length)
					return parser(child,rest,tokens)
				}
				else {
					node.word = tokens.head
					node.dependents = dependencies.getOrElse(tokens.head.id,List())
					var n = node
					for (i <- 1 to (w count { c => if (c == ')') true else false }))
						n = n.parent
					return parser(n,rest,tokens tail)
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
	
	object Relations	{
		def abbrev(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("abbrev")
			
			return null
		}
		
		def acomp(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("acomp")
			return null
		}
		
		def advcl(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("advcl")
			return null
		}
		
		def advmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("advmod")
			return null
		}
		
		def agent(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("agent")
			return null
		}
		
		def amod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("amod")
			return null
		}
		
		def appos(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("appos")
			return null
		}
		
		def attr(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("attr")
			return null
		}
		
		def aux(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("aux")
			return null
		}
		
		def auxpass(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("auxpass")
			return null
		}
		
		def cc(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("cc")
			return null
		}
		
		def ccomp(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("ccomp")
			return null
		}
		
		def complm(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("complm")
			return null
		}
		
		def conj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("conj")
			return null
		}
		
		def cop(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("cop")
			return null
		}
		
		def csubj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("csubj")
			return null
		}
		
		def csubjpass(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("csubjpass")
			return null
		}
		
		def dep(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("dep")
			return null
		}
		
		def det(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("det")
			return null
		}
		
		def dobj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("dobj")
			return null
		}
		
		def expl(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("expl")
			return null
		}
		
		def infmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("infmod")
			return null
		}
		
		def iobj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("iobj")
			return null
		}
		
		def mark(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("mark")
			return null
		}
		
		def mwe(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("mwe")
			return null
		}
		
		def neg(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("neg")
			return null
		}
		
		def nn(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("nn")
			return null
		}
		
		def npadvmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("npadvmod")
			return null
		}
		
		def nsubj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("nsubj")
			return null
		}
		
		def nsubjpass(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("nsubjpass")
			return null
		}
		
		def num(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("num")
			return null
		}
		
		def number(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("number")
			return null
		}
		
		def parataxis(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("parataxis")
			return null
		}
		
		def partmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("partmod")
			return null
		}
		
		def pcomp(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("pcomp")
			return null
		}
		
		def pobj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("pobj")
			return null
		}
		
		def poss(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("poss")
			return null
		}
		
		def possessive(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("possessive")
			return null
		}
		
		def preconj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("preconj")
			return null
		}
		
		def predet(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("predet")
			return null
		}
		
		def prep(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("prep")
			return null
		}
		
		def prepc(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("prepc")
			return null
		}
		
		def prt(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("prt")
			return null
		}
		
		def punct(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("punct")
			return null
		}
		
		def purpcl(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("purpcl")
			return null
		}
		
		def quantmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("quantmod")
			return null
		}
		
		def rcmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("rcmod")
			return null
		}
		
		def ref(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("ref")
			return null
		}
		
		def rel(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("rel")
			return null
		}
		
		def tmod(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("tmod")
			return null
		}
		
		def xcomp(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("xcomp")
			return null
		}
		
		def xsubj(gov: ParseTreeNode, dep:ParseTreeNode): ParseTreeNode = {
			println("xsubj")
			return null
		}
		
		def handler(relType: String): (ParseTreeNode,ParseTreeNode) => ParseTreeNode = relType match  {
			case "abbrev" 		=> return abbrev
			case "acomp"  		=> return acomp
			case "advcl"		=> return advcl
			case "advmod"		=> return advmod
			case "agent"		=> return agent
			case "amod"			=> return amod
			case "appos"		=> return appos
			case "attr"			=> return attr
			case "aux"			=> return aux
			case "auxpass"		=> return auxpass
			case "cc"			=> return cc
			case "ccomp"		=> return ccomp
			case "complm"		=> return complm
			case "conj"			=> return conj
			case "cop"			=> return cop
			case "csubj"		=> return csubj
			case "csubjpass"	=> return csubjpass
			case "dep"			=> return dep
			case "det"			=> return det
			case "dobj"			=> return dobj
			case "expl"			=> return expl
			case "infmod"		=> return infmod
			case "iobj"			=> return iobj
			case "mark"			=> return mark
			case "mwe"			=> return mwe
			case "neg"			=> return neg
			case "nn"			=> return nn
			case "npadvmod"		=> return npadvmod
			case "nsubj"		=> return nsubj
			case "nsubjpass"	=> return nsubjpass
			case "num"			=> return num
			case "number"		=> return number
			case "parataxis"	=> return parataxis
			case "partmod"		=> return partmod
			case "pcomp"		=> return pcomp
			case "pobj"			=> return pobj
			case "poss"			=> return poss
			case "possessive"	=> return possessive
			case "preconj"		=> return preconj
			case "predet"		=> return predet
			case "prep"			=> return prep
			case "prepc"		=> return prepc
			case "prt"			=> return prt
			case "punct"		=> return punct
			case "purpcl"		=> return purpcl
			case "quantmod"		=> return quantmod
			case "rcmod"		=> return rcmod
			case "ref"			=> return ref
			case "rel"			=> return rel
			case "tmod"			=> return tmod
			case "xcomp"		=> return xcomp
			case "xsubj"		=> return xsubj
			case _				=> return dep
		}
	
	}
	


}

object Sentence	{

/*
	val tokens: List[Token] = (List[Token]() /: tokNode) (_ :+ Token.fromXML(_))
	val dependencies: List[Dependency] = (List[Dependency]() /: depNode) (_ :+ Dependency.fromXML(_))
*/
	
	def fromXML(node: Node): Sentence =
		new Sentence {
			val tokens = (List[Token]() /: (node \\ "token")) (_ :+ Token.fromXML(_))
			val dependencies = 	(Map[String,List[Dependency]]() /: depNode) (
				(map:Map[String,List[Dependency]],dep:Node) => { map + (((dep \ "governor" \ "@idx").text) -> 																			(map.getOrElse(((dep \ "governor" \ "@idx").text),List()) 
											++ List(Dependency.fromXML(dep))))})
			val parseTree = ParseTreeNode.parse(node \\ "parse" text)
		}

}


