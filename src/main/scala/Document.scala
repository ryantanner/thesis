package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/23/11
 * Time: 8:57 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Document {

	val sentences: List[Sentence]
	val aliases: Map[Alias,List[Alias]]
	val filePath: String

	def resolve(alias:Alias): String = {
		return sentences(alias.sentence-1).tokens.slice(alias.start,alias.end).mkString(" ")
	}

	def printAllAliases()   {
		aliases map { a => printf("%s: %s\n\n",resolve(a._1),(a._2 map { a2 => resolve(a2) }).mkString(";"))}
	}
        

        def aliasProps(): Map[Alias,List[Property]] = {
          val fal = aliases map { kv => kv._2 :+ kv._1 } reduceLeft { (acc,l) => acc ++ l }
          val aps = sentences map {
	          s => val ap = (s.properties map {
		          tp => fal filter { _.tokenIsInRange(tp._1,s.id) } map { (_ -> tp._2) }
	          }).foldLeft(List[(thesis.Alias, List[thesis.Property])]())((acc,l) => acc ++ l); ap
          }
          val mps = aps map {
	          ap => (Map[Alias,List[Property]]() /: ap) { reduceMap(_,_) }
                } reduceLeft { (acc,m) => (acc /: m) (reduceMap(_,_)) }
	      //mps map { mp => (mp._1 -> (mp._2 reduceLeft { p =>  }))}
          // Needs code to take all the list kv pairs in aps and make a new map using getOrElse
          //return aps map { _ filter { _._1.representative == false } map { kv => (kv._1.rep -> (mps.getOrElse(kv._1
		  //        .rep,List[Property]()) ++ kv._2)) } }
	      return mps
        }

		private def reduceMap(acc: Map[Alias,List[Property]], l: (Alias,List[Property])): Map[Alias,List[Property]] = {
			acc + (l._1 -> (acc.getOrElse(l._1,List[Property]()) ++ l._2))
		}

		def nerFilter(filter: String = "O"): Map[Alias,List[Property]] = {
		      (Map[Alias,List[Property]]() /: aliasProps) { (acc,m) => acc + (m._1 -> (m._2 filter { p => p.quality
				      .ner != filter} ))}
		}
		
		def printConnections = {
			nerFilter() map { ner => println("\n" + resolve(ner._1)); 
				ner._2 map { 
					p => println("\t" + (aliases.values.toList map { _ filter { _.sentence == p.quality.sent } } filter { _.size > 0 } map { _ map { resolve(_) } }));
						 println("\t\t" + sentences(p.quality.sent).tokens.mkString(" ")); } }
		}

        def conMap = {
            nerFilter() map { case (ner, props) =>
                // Making a map of tuples: (Entity stuff) -> (dependent entities, properties connecting them)
                (resolve(ner), true) -> ((props map { p => sentences(p.quality.sent).tokens }).flatten,
                  Utilities.multiFlatten((props map { p => aliases.values.toList map { _ filter { _.sentence == p.quality.sent } } filter { _.size > 0 } map { _ map { resolve(_) } } } )))
            }
        }
		
		def connections = {
			nerFilter() map { ner => (ner._1 -> (ner._2 map { p => aliases.values.toList map { _ filter { _.sentence == p.quality.sent }} filter {
				_.size > 0} apply(0) apply(0) } )) }
		}
		
/*		def aliasesForToken(tok: Token): List[Alias] = {
		
		 }*/


}

object Document {

	def fromXML(node: xml.NodeSeq, fileName: String): Document = {
		return new Document {
			val sentences = ((node \ "document" \ "sentences" \\  "sentence") map { s => Sentence
					.fromXML(s)}).toList
			var temp = (node \ "document" \ "coreference" \ "coreference") map { cof => (cof \\ "mention") map { Alias.fromXML(_) } } 
                        temp foreach { t => t.tail map { a => a.rep = t.head }}
			val aliases = Map[Alias,List[Alias]]() ++ (temp map { l => (l filter { _.representative == true } apply
					(0)) -> (l filter { _.representative == false } toList)})
			
            val filePath = fileName       
		}
	}
        
        def fromFile(file: String = "data/ww2sample.txt.xml"): Document = {
			return fromFile(new java.io.File(file))
        }

		def fromFile(file: java.io.File): Document = {
			import scala.xml.parsing.ConstructingParser

			val p = ConstructingParser.fromFile(file, true /*preserve whitespace*/)
			val d: scala.xml.Document = p.document()

			return Document.fromXML(d, file.getPath)			
		}

}
