package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 9/18/11
 * Time: 7:55 PM
 * To change this template use File | Settings | File Templates.
 */

object Main {

	def main(args: Array[String]): Unit = {

	}

	def init(): Sentence = {
		import scala.xml._

		val loadFile = XML.loadFile("data/ww2sent.txt.xml")

		val sent = Sentence.fromXML(loadFile)

		println(sent)
		return sent
	}

}