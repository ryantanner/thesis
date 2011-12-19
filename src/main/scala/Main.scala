package thesis

import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/18/11
 * Time: 7:55 PM
 * To change this template use File | Settings | File Templates.
 */

object Main {


	def init(file: String = "data/ww2sent.txt.xml"): Sentence = {
		import scala.xml._

		val loadFile = XML.loadFile(file)

		val sents = loadFile \\ "sentence"



		val sent = Sentence.fromXML(loadFile)

		println(sent)
		return sent
	}

	def proc(sent: Sentence): Map[Token,List[Property]] = {
		// only works for ww2sent.txt
		if (sent.root.length > 0)
			return (Map[Token,List[Property]]() /: (sent.dependencies(sent.root(0)) map { e => e.relFunc(e.gov,
				e.dep) } )) { (acc,dep) => (acc /: dep) { (sacc,kv) => sacc + (kv._1 -> (sacc.getOrElse(kv._1,List[Property]()) ++ kv._2)) } }
		return Map()
	}

	def subj(deps: Map[Token,List[Property]], sent:Sentence): Template = {
		if (sent.root.length > 0)
		return new Template((deps filter { kv => !(kv._2 filter { v => v.getClass == classOf[Relation] }).isEmpty })
				(sent.tokens(sent.root(0))))
		return new Template(List[Property]())
	}

	def initMany(file: String = "data/ww2sample.txt.xml"): List[Sentence] = {
		return initMany(new File(file))
	}
	
	def initMany(file: java.io.File): List[Sentence] = {
		import scala.xml.parsing.ConstructingParser

		val p = ConstructingParser.fromFile(file, true /*preserve whitespace*/)
		val d: scala.xml.Document = p.document()


		return ((d \ "document" \ "sentences" \\  "sentence") map { s => 
			try { 
			  Sentence.fromXML(s)
			} catch {
			  	case e: NullPointerException => println(e); new EmptySentence()
				case _ => new EmptySentence()
			}
		}).toList filter { _.getClass != classOf[EmptySentence] }
	}
        
  

}