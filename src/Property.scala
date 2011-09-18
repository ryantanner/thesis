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
}

class IsOfType extends Property with IsOf {

}


trait IsOf
trait HasA
