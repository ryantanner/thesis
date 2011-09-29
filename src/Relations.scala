package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */



object Relations	{
  class dep extends Function2[ParseTreeNode, ParseTreeNode, Map[Token,List[Property]]]	{
    // Most basic dependency, all others inherit from this
    def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
	  println(gov + "\t" + dep)
      println("dep")
	  // use dep.dependents to make list of PTNs which are the dependents of dep, reduce over this calling relFunc to produce props
      //val props = (List[Property]() /: Sentence.sent.dependencies.getOrElse(dep.word.id,List())) { (list,
                                                                                                    // rel) => list ++
		//      rel.relFunc.apply(dep,rel.dep)(gov.token) }

	  return  Sentence.sent.dependencies.getOrElse(dep.word.id,List()) map { rel => rel.relFunc.apply(dep,
		  rel.dep) }.foldLeft(Map[Token,List[Property]]) ((acc,m) => (acc /: m) { (map,
	                                                                          kv) => map + (kv._1 -> (map.getOrElse(kv._1,
		  List[Property]()) ++ kv._2)) } )

      //return Map((gov.token -> props))
    }


  }


  class aux extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("aux")
      return super.apply(gov,dep)
    }
  }

  class auxpass extends aux	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("auxpass")
      return super.apply(gov,dep)
    }
  }

  class cop extends aux	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("cop")
      return super.apply(gov,dep)
    }
  }

  class arg extends dep {
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class agent extends arg	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("agent")
      return super.apply(gov,dep)
    }
  }

  class comp extends arg {
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class acomp extends comp	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("acomp")
      return super.apply(gov,dep)
    }
  }

  class abbrev extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("abbrev")
      return super.apply(gov,dep)
    }
  }



  class advcl extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("advcl")
      return super.apply(gov,dep)
    }
  }

  class advmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("advmod")
      return super.apply(gov,dep)
    }
  }



  class amod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)

      return props + (gov.word -> (new IsOfType(dep.word) :: props.getOrElse(gov.word,List[Property]())))

      println("amod")
      return props
    }
  }

  class appos extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)

	    return props + (gov.word -> (new IsOfType(dep.word) :: props.getOrElse(gov.word,List[Property]())))

      println("appos")
      return props
    }
  }

  class attr extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("attr")
      return super.apply(gov,dep)
    }
  }

  class cc extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("cc")
      return super.apply(gov,dep)
    }
  }

  class ccomp extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("ccomp")
      return super.apply(gov,dep)
    }
  }

  class complm extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("complm")
      return super.apply(gov,dep)
    }
  }

  class conj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("conj")
      return super.apply(gov,dep)
    }
  }

  class csubj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("csubj")
      return super.apply(gov,dep)
    }
  }

  class csubjpass extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("csubjpass")
      return super.apply(gov,dep)
    }
  }

  class det extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("det")
      return super.apply(gov,dep)
    }
  }

  class dobj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("dobj")
      return super.apply(gov,dep)
    }
  }

  class expl extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("expl")
      return super.apply(gov,dep)
    }
  }

  class infmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("infmod")
      return super.apply(gov,dep)
    }
  }

  class iobj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("iobj")
      return super.apply(gov,dep)
    }
  }

  class mark extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("mark")
      return super.apply(gov,dep)
    }
  }

  class mwe extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("mwe")
      return super.apply(gov,dep)
    }
  }

  class neg extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("neg")
      return super.apply(gov,dep)
    }
  }

  class nn extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
        println("nn")
        val props = super.apply(gov,dep)
	    return props + (gov.word -> (props.getOrElse(gov.word,List[Property]()) :+ new NounProperty(dep.word)))
    }
  }

  class npadvmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("npadvmod")
      return super.apply(gov,dep)
    }
  }

  class nsubj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("nsubj")

	    val props = super.apply(gov,dep)
	    //dep is "thing"
	    //    dep should be some property saying it is a "thing" for a clause
	    val nns = props(dep.word) filter { _.getClass == classOf[NounProperty] } map { np:Property => np
			    .asInstanceOf[NounProperty] } reduceRight { (np:NounProperty,nn:NounProperty) => nn ++ np }

	    return props + (dep.word -> (nns :: (props(dep.word) filterNot { p => p.getClass == classOf[NounProperty] })))
      //return super.apply(gov,dep)
    }
  }

  class nsubjpass extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("nsubjpass")
      return super.apply(gov,dep)
    }
  }

  class num extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("num")
      return super.apply(gov,dep)
    }
  }

  class number extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("number")
      return super.apply(gov,dep)
    }
  }

  class parataxis extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("parataxis")
      return super.apply(gov,dep)
    }
  }

  class partmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("partmod")
      return super.apply(gov,dep)
    }
  }

  class pcomp extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("pcomp")
      return super.apply(gov,dep)
    }
  }

  class pobj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("pobj")
      return super.apply(gov,dep)
    }
  }

  class poss extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("poss")
      return super.apply(gov,dep)
    }
  }

  class possessive extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("possessive")
      return super.apply(gov,dep)
    }
  }

  class preconj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("preconj")
      return super.apply(gov,dep)
    }
  }

  class predet extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("predet")
      return super.apply(gov,dep)
    }
  }

  class prep extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("prep")
      return super.apply(gov,dep)
    }
  }

  class prepc extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("prepc")
      return super.apply(gov,dep)
    }
  }

  class prt extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("prt")
      return super.apply(gov,dep)
    }
  }

  class punct extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("punct")
      return super.apply(gov,dep)
    }
  }

  class purpcl extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("purpcl")
      return super.apply(gov,dep)
    }
  }

  class quantmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("quantmod")
      return super.apply(gov,dep)
    }
  }

  class rcmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("rcmod")
      return super.apply(gov,dep)
    }
  }

  class ref extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("ref")
      return super.apply(gov,dep)
    }
  }

  class rel extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("rel")
      return super.apply(gov,dep)
    }
  }

  class tmod extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("tmod")
      return super.apply(gov,dep)
    }
  }

  class xcomp extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("xcomp")
      return super.apply(gov,dep)
    }
  }

  class xsubj extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("xsubj")
      return super.apply(gov,dep)
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