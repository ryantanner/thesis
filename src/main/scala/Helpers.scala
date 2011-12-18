package thesis

import java.io.File

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 11/17/11
 * Time: 10:24 PM
 * To change this template use File | Settings | File Templates.
 */

object Helpers {

}

class RichFile( file: java.io.File ) {
    import java.io.PrintWriter
	import scala.io.Source
    def text = Source.fromFile( file ).mkString

	def text_=( s: String ) {
		val out = new PrintWriter( file )
		try{ out.println( s ) }
		finally{ out.close }
	}
}

object RichFile {

    implicit def enrichFile( file: java.io.File ) = new RichFile( file )
	implicit def enrichFile( string: String) = new RichFile( new File(string) )

}