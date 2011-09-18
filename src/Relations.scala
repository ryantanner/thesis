package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */



object Relations	{
  class dep extends Function2[ParseTreeNode, ParseTreeNode, List[Property]]	{
    // Most basic dependency, all others inherit from this
    def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
      println("dep")
      val props = (List[Property]() /: dep.dependents) { _ ++  _.relFunc.apply }
      return props
    }
  }


  class aux extends dep	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("aux")
      return null
    }
  }

  class auxpass extends aux	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("auxpass")
      return null
    }
  }

  class cop extends aux	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("cop")
      return null
    }
  }

  class arg extends dep {
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class agent extends arg	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("agent")
      return null
    }
  }

  class comp extends arg {
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class acomp extends comp	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("acomp")
      return null
    }
  }

  class abbrev extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): List[Property] = {
      println("abbrev")
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



  class amod extends dep	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      val props = super.apply(gov,dep)

      return props ++ new IsOfType(dep.word)

      println("amod")
      return props
    }
  }

  class appos extends dep	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      val props = super.apply(gov,dep)

      return props ++ new IsOfType(dep.word)

      println("appos")
      return props
    }
  }

  class attr extends dep	{
    override def apply(gov: ParseTreeNode, dep:ParseTreeNode): List[Property] = {
      println("attr")
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