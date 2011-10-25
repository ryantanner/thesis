package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/3/11
 * Time: 11:05 PM
 * To change this template use File | Settings | File Templates.
 */

class Template(list: List[Property]) {
	import scala.collection.immutable.TreeSet
	val order = Ordering.fromLessThan[Property](_ < _)
	val subjs = TreeSet.empty[Property](order) ++ (list filter { _.getClass == classOf[Subject] }  map { case
		p:Subject => p })

	val props = TreeSet.empty[Property](order) ++ list filterNot { _.getClass == classOf[Subject] } filterNot { _
			.getClass ==
			classOf[Relation] }
	val rel = TreeSet.empty[Property](order) ++ (list filter { _.getClass == classOf[Relation] } map {
		case p:Relation => p })

	val time = TreeSet.empty[Property](order) ++ (list filter { _.getClass == classOf[DateRange] } map {
		case p:DateRange => p })

	override def toString:String = subjs.mkString(" AND ") + " " + rel.mkString(" ," +
			"").toUpperCase + " AND " + props.mkString(" AND ") + " FOR " + time.mkString(" AND ")

}