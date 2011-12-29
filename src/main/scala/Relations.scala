package thesis;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */


class Relations(s: Sentence)    {

	val sentence:Sentence = s

}

object Relations	{


	// These two functions are used to add properties to the map.  This just eliminates the getOrElse from being
	// littered all over this object
	def add(map: Map[Token,List[Property]], tok: Token, prop: Property): Map[Token,List[Property]] =   {
		val tokValues = map.getOrElse(tok,List[Property]())
		var nProp = prop
		if (!(tokValues isEmpty))   {
			// checking for duplicate properties
			if (tokValues contains prop)    {
				nProp = tokValues filter { _.equals(prop) } reduceLeft { (acc,p) => p.increment }
			}
		}
		return map + (tok -> (map.getOrElse(tok,List[Property]()) :+ nProp))
	}

	def add(map: Map[Token,List[Property]], tok: Token, prop: Property, rmFunc: (Property) => Boolean):
		Map[Token,List[Property]] =   {
		// This one is a bit more complicated.  rmFunc is passed to a filterNot on the getOrElse to remove properties
		// that we don't want in the props map anymore.  nsubj is a good example of why this is needed.
		val tokValues = map.getOrElse(tok,List[Property]())
		var nProp = prop
		if (!(tokValues isEmpty))   {
			// checking for duplicate properties
		 	if (tokValues contains prop)    {
				nProp = tokValues filter { _.equals(prop) } reduceLeft { (acc,p) => p.increment }
			}
		}
		return map + (tok -> (nProp :: (map.getOrElse(tok,List[Property]()) filterNot rmFunc)))
	}

  class dep extends Function2[ParseTreeNode, ParseTreeNode, Map[Token,List[Property]]]	{
    // Most basic dependency, all others inherit from this
    def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
	  println(gov + "\t" + dep)
      //println("dep")
	  // use dep.dependents to make list of PTNs which are the dependents of dep, reduce over this calling relFunc to produce props
      //val props = (List[Property]() /: Sentence.sent.dependencies.getOrElse(dep.word.id,List())) { (list,
                                                                                                    // rel) => list ++
		//      rel.relFunc.apply(dep,rel.dep)(gov.token) }

	  if (gov.word.id == dep.word.id)   {
		  return Map[Token,List[Property]]()
	  }

	  return  (Map[Token,List[Property]]() /: (Sentence.sent.dependencies.getOrElse(dep.word.id,
		  List()) map { rel => rel.relFunc.apply(dep,
		  rel.dep) }) ) ((acc,m) => (acc /: m) { (map,kv) => map + (kv._1 -> (map.getOrElse(kv._1,List[Property]()) ++ kv._2)) } )

      //return Map((gov.token -> props))
    }


  }


  class aux extends dep	{
	// Auxillary
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("aux")
		/* Because aux handles the relationship between the main verb of a sentence and non-main verb,
		 * e.g., "has died" becomes aux(died,has), this provides only tense information.  Unlikely to be
		 * useful and disregarded for now.
		 */
      return super.apply(gov,dep)
    }
  }

  class auxpass extends aux	{
	// Passive auxillary
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("auxpass")
/* Similar to aux, this is the passive verb in (e.g.) "has been killed" such that auxpass(killed,been) */
      return super.apply(gov,dep)
    }
  }

  class cop extends aux	{
	// Copula
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("cop")
		val props = super.apply(gov,dep)
		/* Connects be-verbs to their subjects.  VERY useful.  */
	    return add(add(props,gov.word,new Relation(dep.word)),gov.word,new IsOfType(gov.word))
    }
  }

  class arg extends dep {
	// Argument
	// Pass-through, only here for OOP purposes
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class agent extends arg	{
	// Agent
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("agent")
		/* Provides "extra" information to the verb
		 * e.g., "The man has been killed by the police" agent(killed,police)
		 * Introduced by preposition "by"
		 */
		val props = super.apply(gov,dep)

		return add(props,gov.word,new AgentRelation(gov.word,dep.word))
//      return super.apply(gov,dep)
    }
  }

  class comp extends arg {
	// Complement
	// Only here for OOP purposes
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)
      return props
    }
  }

  class acomp extends comp	{
	// Adjectival complement
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("acomp")
/* Object of the verb
 * "She looks beautiful" acomp(looks,beautiful)
*/
      return add(super.apply(gov,dep),gov.word,new IsOfType(dep.word))
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
	    println("amod")

      //return props + (gov.word -> (new IsOfType(dep.word) :: props.getOrElse(gov.word,List[Property]())))
	  return add(props,gov.word,new IsOfType(dep.word))
    }
  }

  class appos extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      val props = super.apply(gov,dep)
	    println("appos")

	    return add(props,gov.word,new IsOfType(dep.word))
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
	    //return props + (gov.word -> (props.getOrElse(gov.word,List[Property]()) :+ new NounProperty(dep.word)))
	    return add(props,gov.word,new NounProperty(dep.word))
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
	    val nns = (new NounProperty(dep.word) /: (props.getOrElse(dep.word,List[Property]()) filter { _.getClass ==
			    classOf[NounProperty] } map { np:Property => np match { case np2: NounProperty => np2; case _ =>
		    throw new ClassCastException } } ) ) { (np:NounProperty,nn:NounProperty) => nn ++ np }

	    //return props + (dep.word -> (nns :: (props.getOrElse(dep.word,List[Property]()) filterNot { p => p.getClass
	    // ==
		//	    classOf[NounProperty] })))
	    val temp = add(props,dep.word,nns,{ p:Property => p.getClass == classOf[NounProperty] })
	    return add(temp,gov.word,new Subject(nns))
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
			val props = super.apply(gov, dep)
			val nm = props.getOrElse(dep.word, List())
			if (!nm.isEmpty)    {
				return nm(0) match {
					case a: NumberModifier => add(props,gov.word,new Quantity(a,List(gov.word)),
							{ p:Property => p.getClass ==
							classOf[NumberModifier]})
					case _ => add(props,gov.word,new Number(dep.word))
				}
			}
			return add(props,gov.word,new Number(dep.word))
		}
  }

  class number extends dep	{
    override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
      println("number")
	    val props = super.apply(gov,dep)

	  return  add(props,gov.word,new NumberModifier(dep.word, List(gov.word)))

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

	class conj_or extends conj  {
		override def apply(gov: ParseTreeNode, dep:ParseTreeNode): Map[Token,List[Property]] = {
			println("conj_or")
			val props = super.apply(gov,dep)

			return add(props,gov.word,new AlternativePhrase(dep.word))
		}
	}

	class prep_as extends prep  {
		override def apply(gov: ParseTreeNode, dep:ParseTreeNode): Map[Token,List[Property]] = {
			println("prep_as")
			val props = super.apply(gov,dep)

			return add(props,gov.word,new As(dep.word))
		}
	}

	class prep_from extends prep    {
		override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
			println("prep_from")
			val props = super.apply(gov, dep)

			if (dep.word.ner == "DATE") {
				 // get deps whose rel is num
				// turn into date

				/*val date = new DateRange(dep.word, (dep.dependents map { Sentence.sent
						.dependencies.getOrElse(_,List[Dependency]()) } reduceLeft { (acc,
				                                                                      l) => acc ++ (l filter { _
						.relType == "num"})}).head.dep.word)*/
				if (props.isDefinedAt(dep.word))	{
					val date = new DateRange(dep.word,props(dep.word)(0).quality)
					return add(props,gov.word,date)
				}
			}
			return props

		}
	}

	class prep_with extends prep  {
		override def apply(gov: ParseTreeNode, dep:ParseTreeNode): Map[Token,List[Property]] = {
			println("prep_with")
			val props = super.apply(gov,dep)

			return add(props,gov.word,new PartsOfEntity(List(dep.word)))
		}
	}

	class prep_in extends prep  {
		override def apply(gov: ParseTreeNode, dep:ParseTreeNode): Map[Token,List[Property]] = {
			println("prep_with")
			val props = super.apply(gov,dep)
			return props
			//return add(props,gov.word,new PartsOfEntity(List(dep.word)))
		}
	}

//	class prep_including extends prep   {
//		override def apply(gov: ParseTreeNode, dep: ParseTreeNode): Map[Token,List[Property]] = {
//			println("prep_including")
//			val props = super.apply(gov, dep)
//
//			val parts = new PartsOf
//			//return add(props,gov.word,new PartsOfEntity(props.getOrElse(dep.word)))
//		}
//	}


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
	case "conj_or"      => return new conj_or
	case "prep_as"      => return new prep_as
	case "prep_from"    => return new prep_from
	case "prep_with"    => return new prep_with
	case "prep_in"      => return new prep_in
    case _				=> return new dep
  }

}