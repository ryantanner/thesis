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
        var rep:Alias
        
        def tokenIsInRange(idx: Int, s: Int): Boolean = {
          if (idx >= start && idx < end && s == sentence)
            return true;
          else
            return false;
        }
        
        def tokenIsInRange(tok: Token, s: Int): Boolean = {
          if (tok.id >= start && tok.id < end && s == sentence)
            return true;
          else
            return false;          
        }

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
                        var rep:Alias = new EmptyAlias()
		}
	}

}

class EmptyAlias extends Alias  {
	val representative:Boolean = false
	val sentence:Int = 0
	val start:Int = 0
	val end:Int = 0
	val head:Int = 0
        var rep:Alias = null
}