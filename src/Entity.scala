package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/20/11
 * Time: 9:33 PM
 * To change this template use File | Settings | File Templates.
 */

class Entity(ts:List[Token]) {

	val toks = ts
	val begin = ts.head.id
	val end = ts.last.id

	override def toString = toks.mkString(" ")
}

object Entity   {
	//val tokList = ((loadNode \\ "token") toList) map { tok => List(tok) }
	private def groupNer(list: List[List[Token]]): List[List[Token]] = list match {
	    case List() => List()
	    case d1 :: d2 :: rest =>
	        if (d1.last.ner == d2.last.ner)
	            groupNer((d1 ::: d2) :: rest)
	        else
	            d1 :: groupNer(d2 :: rest)
	    case d1 :: rest => d1 :: groupNer(rest)
	}
	//val ners = groupNer(tokList) filter { l => (l.head \ "NER").text != "O" }

	def entityMap(list: List[Token]): Map[Entity,List[Property]] = {
		// Basically, take the list from groupNer, remove whatever we don't care about, map whatever is left
		// to entity objects and turn that into a map (entity -> list[property])
		return ((groupNer(list map { tok => List(tok) })
				filter { l => l.head.ner != "O" & !l.isEmpty }
				map { new Entity(_) }
				map { (_ -> List[Property]()) }) :\ Map[Entity,List[Property]]()) ( (l,m) => m + l)
	}
}