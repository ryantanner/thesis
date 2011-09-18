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
		val relFunc: Relations.dep
		
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
		class dep extends Function2[ParseTreeNode, ParseTreeNode, List[Property]]	{
			// Most basic dependency, all others inherit from this
			def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
				println("dep")
				return null
			}
		}

		class abbrev extends dep	{
			override def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
				println("abbrev")
				return null	
			}
		}
		
		class acomp extends dep	{ 
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("acomp")
				return null
			}
		}
		
		class advcl extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("advcl")
				return null
			}
		}
		
		class advmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("advmod")
				return null
			}
		}
		
		class agent extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("agent")
				return null
			}
		}
		
		class amod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				
				println("amod")
				return null
			}
		}
		
		class appos extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("appos")
				return null
			}
		}
		
		class attr extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("attr")
				return null
			}
		}
		
		class aux extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("aux")
				return null
			}
		}
		
		class auxpass extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("auxpass")
				return null
			}
		}
		
		class cc extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("cc")
				return null
			}
		}
		
		class ccomp extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("ccomp")
				return null
			}
		}
		
		class complm extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("complm")
				return null
			}
		}
		
		class conj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("conj")
				return null
			}
		}
		
		class cop extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("cop")
				return null
			}
		}
		
		class csubj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("csubj")
				return null
			}
		}
		
		class csubjpass extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("csubjpass")
				return null
			}
		}
		
		class det extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("det")
				return null
			}
		}
		
		class dobj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("dobj")
				return null
			}
		}
		
		class expl extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("expl")
				return null
			}
		}
		
		class infmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("infmod")
				return null
			}
		}
		
		class iobj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("iobj")
				return null
			}
		}
		
		class mark extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("mark")
				return null
			}
		}
		
		class mwe extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("mwe")
				return null
			}
		}
		
		class neg extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("neg")
				return null
			}
		}
		
		class nn extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("nn")
				return null
			}
		}
		
		class npadvmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("npadvmod")
				return null
			}
		}
		
		class nsubj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("nsubj")
				return null
			}
		}
		
		class nsubjpass extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("nsubjpass")
				return null
			}
		}
		
		class num extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("num")
				return null
			}
		}
		
		class number extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("number")
				return null
			}
		}
		
		class parataxis extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("parataxis")
				return null
			}
		}
		
		class partmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("partmod")
				return null
			}
		}
		
		class pcomp extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("pcomp")
				return null
			}
		}
		
		class pobj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("pobj")
				return null
			}
		}
		
		class poss extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("poss")
				return null
			}
		}
		
		class possessive extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("possessive")
				return null
			}
		}
		
		class preconj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("preconj")
				return null
			}
		}
		
		class predet extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("predet")
				return null
			}
		}
		
		class prep extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("prep")
				return null
			}
		}
		
		class prepc extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("prepc")
				return null
			}
		}
		
		class prt extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("prt")
				return null
			}
		}
		
		class punct extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("punct")
				return null
			}
		}
		
		class purpcl extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("purpcl")
				return null
			}
		}
		
		class quantmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("quantmod")
				return null
			}
		}
		
		class rcmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("rcmod")
				return null
			}
		}
		
		class ref extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("ref")
				return null
			}
		}
		
		class rel extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("rel")
				return null
			}
		}
		
		class tmod extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("tmod")
				return null
			}
		}
		
		class xcomp extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("xcomp")
				return null
			}
		}
		
		class xsubj extends dep	{
			override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
				println("xsubj")
				return null
			}
		}
		
		def handler(relType: String): dep = relType match  {
			case "abbrev" 		=> return new abbrev
			case "acomp"  		=> return new acomp
			case "advcl"		=> return new advcl
			case "advmod"		=> return new advmod
			case "agent"		=> return new agent
			case "amod"			=> return new amod
			case "appos"		=> return new appos
			case "attr"			=> return new attr
			case "aux"			=> return new aux
			case "auxpass"		=> return new auxpass
			case "cc"			=> return new cc
			case "ccomp"		=> return new ccomp
			case "complm"		=> return new complm
			case "conj"			=> return new conj
			case "cop"			=> return new cop
			case "csubj"		=> return new csubj
			case "csubjpass"	=> return new csubjpass
			case "dep"			=> return new dep
			case "det"			=> return new det
			case "dobj"			=> return new dobj
			case "expl"			=> return new expl
			case "infmod"		=> return new infmod
			case "iobj"			=> return new iobj
			case "mark"			=> return new mark
			case "mwe"			=> return new mwe
			case "neg"			=> return new neg
			case "nn"			=> return new nn
			case "npadvmod"		=> return new npadvmod
			case "nsubj"		=> return new nsubj
			case "nsubjpass"	=> return new nsubjpass
			case "num"			=> return new num
			case "number"		=> return new number
			case "parataxis"	=> return new parataxis
			case "partmod"		=> return new partmod
			case "pcomp"		=> return new pcomp
			case "pobj"			=> return new pobj
			case "poss"			=> return new poss
			case "possessive"	=> return new possessive
			case "preconj"		=> return new preconj
			case "predet"		=> return new predet
			case "prep"			=> return new prep
			case "prepc"		=> return new prepc
			case "prt"			=> return new prt
			case "punct"		=> return new punct
			case "purpcl"		=> return new purpcl
			case "quantmod"		=> return new quantmod
			case "rcmod"		=> return new rcmod
			case "ref"			=> return new ref
			case "rel"			=> return new rel
			case "tmod"			=> return new tmod
			case "xcomp"		=> return new xcomp
			case "xsubj"		=> return new xsubj
			case _				=> return new dep
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
			val dependencies = 	(Map[String,List[Dependency]]() /: (node \\ "basic-dependencies" \\ "dep")) (
				(map:Map[String,List[Dependency]],dep:Node) => { map + (((dep \ "governor" \ "@idx").text) -> 																			(map.getOrElse(((dep \ "governor" \ "@idx").text),List()) 
											++ List(Dependency.fromXML(dep))))})
			val parseTree = ParseTreeNode.parse(node \\ "parse" text)
		}

}


