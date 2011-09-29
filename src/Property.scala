package thesis;

  /**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */

class Properties

class Property(qual:Token) {
  val quality: Token = qual

	override def toString:String = "Has property: " + quality
}

class IsOfType(qual:Token) extends Property(qual:Token) with IsOf {
	override def toString: String = "Is of type: " + quality
}

class NounProperty(qual:List[Token]) extends Property(qual.head:Token) {
	// store head of nn in quality
	import scala.collection.immutable.TreeSet
	val order = Ordering.fromLessThan[Token](_ < _)
	val noms = TreeSet.empty[Token](order) ++ qual

	def this(qual:Token) = this(List(qual))

	def +(qual:Token): NounProperty = new NounProperty((noms + qual).toList)

	def ++(qual:List[Token]): NounProperty = new NounProperty((noms ++ qual).toList)

	def ++(qual:NounProperty): NounProperty = new NounProperty((this.noms ++ qual.noms).toList)

}


trait IsOf
trait HasA
