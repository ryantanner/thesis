package thesis;

  /**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/17/11
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */

class Properties

class Property(qual:Token) extends Ordered[Property] {
  val quality: Token = qual

	def this() = this(new EmptyToken)

	override def toString:String = "Has property: " + quality

	def compare(that: Property) = this.quality.compare(that.quality)

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

	def this() = this(new EmptyToken())

	def +(qual:Token): NounProperty = new NounProperty((noms + qual).toList)

	def ++(qual:List[Token]): NounProperty = new NounProperty((noms ++ qual).toList)

	def ++(qual:NounProperty): NounProperty = new NounProperty((this.noms ++ qual.noms).toList)

	override def toString: String = noms.mkString(" ")

}

class AlternativePhrase(prop: Token) extends Property(prop: Token) {
	override def toString: String = "Or (head): " + quality
	// Indicates that the head of a noun phrase at prop is an "or" clause to whatever holds this property
}

class As(prop: Token) extends Property(prop: Token) {
	override def toString:String = "As (head): " + quality
}

class Relation(prop: Token) extends Property(prop: Token) {
	override def toString:String = "Relates using: " + quality.lemma
}

class Subject(p: NounProperty) extends NounProperty(p.quality: Token) {
	val prop = p
	override def toString:String = "Subject: " + p
}

class DateRange(f: Token, t: Token) extends Property(f: Token) {
	val from = f
	val to = t

	override def toString:String = "Range from " + from + " to " + to
}

class Number(n: Token) extends Property(n: Token)   {
	//val prop = n
	override def toString:String = "Number" + n
}

class Quantity(s: Token, m: List[Token], u: List[Token]) extends Property(s: Token) {
	val size:Token = s
	val modifier:List[Token] = m
	val unit:List[Token] = u

	override def toString:String = "Quantity: " + size.word + " " + modifier.mkString(" ") + " " + unit.mkString(" ")

	def this(nm: NumberModifier, u: List[Token]) = this(nm.size,nm.modifier,u)
}

class NumberModifier(s: Token, m: List[Token]) extends Property(s: Token)   {
	// e.g. "100 million"
	val size:Token = s
	val modifier:List[Token] = m

	override def toString:String = "NM: " + size.word + " " + modifier.mkString(" ")
}

class PartsOfEntity(qual: List[Token]) extends Property(qual.head: Token)  {
	val parts = qual
	override def toString:String = "Constituents: " + parts.mkString(" and part ")
}

trait IsOf
trait HasA
