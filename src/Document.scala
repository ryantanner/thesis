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

	def resolve(alias:Alias): String = {
		return sentences(a.sentence-1).tokens.slice(a.start,a.end).mkString(" ")
	}

	def printAllAliases()   {
		aliases map { a => printf("%s: %s\n\n",resolve(a._1),(a._2 map { a2 => resolve(a2) }).mkString(";"))}
	}

}

object Document {

	def fromXML(node: xml.NodeSeq): Document = {
		return new Document {
			val sentences = ((node \ "document" \ "sentences" \\  "sentence") map { s => Sentence
					.fromXML(s)}).toList
			val temp = (node \ "document" \ "coreference" \ "coreference") map { cof => (cof \\ "mention") map { Alias.fromXML(_) } }
			val aliases = Map[Alias,List[Alias]]() ++ (temp map { l => (l filter { _.representative == true } apply
					(0)) -> (l filter { _.representative == false } toList)})
		}
	}

}