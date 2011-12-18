package thesis

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 11/30/11
 * Time: 7:19 PM
 * To change this template use File | Settings | File Templates.
 */

object Geocoder {

	def apply(query: String): Location = {
        if (query == "" || query == null)	{
			throw new IllegalArgumentException("Query may not be empty or null")
		}

		import dispatch._
        import scala.xml.XML
        
        val h = new Http
		try { 
			val req = url("http://maps.googleapis.com/maps/api/geocode/xml?address=" +
				 			query.replace(" ","+") + 
							"&sensor=false")
			val handler = req
			val resp = h(handler as_str)
	        val locResp = XML.loadString(resp)
	        val status = locResp \\ "status" text
	
			if (status == "ZERO_RESULTS")	{
				throw new RuntimeException("Query is not a valid place")
			}
			else if (status == "OVER_QUERY_LIMIT")	{
				throw new RuntimeException("Over query limit")
			}
			else if (status == "REQUEST_DENIED")	{
				throw new RuntimeException("Request denied")
			}
			else {
		        val lat = locResp \\ "location" \\ "lat" text
		        val lng = locResp \\ "location" \\ "lng" text

				return new Location(lat,lng)
			}
		} catch {
		  	case e: Exception => print("Exception"); throw e;
		}

		
	}

}

class Location(lt: String, lg: String)  {
	val lat:String = lt
	val lng:String = lg

}
