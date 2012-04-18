package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 11/30/11
 * Time: 7:19 PM
 * To change this template use File | Settings | File Templates.
 */

object Geocoder {

	def apply(query: String): Option[Location] = {
        if (query == "" || query == null)	{
			throw new IllegalArgumentException("Query may not be empty or null")
		}

		import dispatch._
        import scala.xml.XML
        
        val h = new Http
        val req = url("http://nominatim.openstreetmap.org/search?q=" +
                        query.replace(" ","+") + 
                        "&format=xml")
        val handler = req
        val resp = h(handler as_str)
        val locResp = XML.loadString(resp)

        if(locResp \ "place" isEmpty) return None

        val lat = (locResp \\ "place")(0) \\ "@lat" text
        val lng = (locResp \\ "place")(0) \\ "@lon" text

        return Some(new Location(lat,lng))

		
	}

    def run:Unit = {
        import thesis.db._
        import org.squeryl.PrimitiveTypeMode._

        ThesisSession.startDbSession()

          transaction {
            val locs = EntityGraph.locations.toList.groupBy(l => l.loc)

            locs.filterNot(lk => lk._1 == "Europe North | Derry").filter(lk => lk._2.exists(l => l.lat.isEmpty)) foreach { case (loc,list) =>
              try {
                println(loc)

                Geocoder(loc) match {
                  case Some(res) => {
                      list foreach { l =>
                        EntityGraph.locations.delete(l.id)
                        ThesisSession.insertLocation(
                          l.loc,
                          l.sentenceId,
                          res.lat.toString,
                          res.lng.toString)
                      }
                  }
                  case None => {}
                }
              } catch {
                case ex => println(ex.getMessage)
              }
          }
        }
    }


}

class Location(lt: String, lg: String)  {
	val lat:String = lt
	val lng:String = lg

}
