package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/23/11
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class Alias {

	val representative:Boolean
	val sentence:Int
	val start:Int
	val end:Int
	val head:Int

}

object Alias    {

	def fromXML(node: scala.xml.Node): Alias =  {
		// pass in a mention node
		return new Alias {
			val representative = node.attribute("representative") match {
				case None => false
				case _ => true
			}
			val sentence = (node \ "sentence").text.toInt
			val start = (node \ "start").text.toInt
			val end = (node \ "end").text.toInt
			val head = (node \ "head").text.toInt
		}
	}

}