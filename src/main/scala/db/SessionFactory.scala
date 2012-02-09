package thesis.db

import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query

object ThesisSession	{
	val dbUser = "thesis"
	val dbPass = "retanner"
	val dbConn = "jdbc:postgresql://sparky.ryantanner.org/thesis"
	
	def startDbSession():Unit = {
        if(!Session.hasCurrentSession)  {
          Class.forName("org.postgresql.Driver")
          SessionFactory.concreteFactory = Some(() => Session.create(
              java.sql.DriverManager.getConnection(dbConn,dbUser,dbPass),
              new PostgreSqlAdapter)
		    )
        }
	}

	def initSchema = {

		startDbSession()

		transaction {
			EntityGraph.create
			println("Created the schema")
		}
	}
	
	def insertDocument(d: thesis.Document): Long = { 
        val ret = EntityGraph.documents.insert(new thesis.db.Document(d.filePath))
        println("Inserted document: " + d.filePath)
        //return retDoc.id
        return ret.id
    }

    def insertSentence(s: thesis.Sentence, dId: Long): Long = {
        val sent = s.tokens.mkString(" ")
        val newS = new Sentence(dId, sent)
        EntityGraph.sentences.insert(newS)
        return newS.id.a1
    }

    def insertAlias(entityValue: String, representative: Boolean, docId: Long, masterId: Option[Long]): Long = {
        val newEnt = EntityGraph.entities.insert(new Entity(entityValue, None, representative, masterId))
        EntityGraph.entitiesFromDocs.insert(new DocumentMatches(newEnt.id, docId))
      println("New entity inserted")
      return newEnt.id
    }

    def getDocumentId(fp: String): Query[Long] = {
        val d = from(EntityGraph.documents)(doc => where(doc.documentPath === fp) select (doc.id))
        return d
    }

    def insertProperty(value: String, entityId: Long): Long = {
        val prop = new Property(value, entityId)
        EntityGraph.properties.insert(prop)
        return prop.id.a2
    }

    def insertQuality(pId: Long, key: String, qual: String, strength: Int) = {
        val p = new PropertyQuality(pId, key, qual, strength)
        EntityGraph.qualitiesOfProperties.insert(p)
    }

    def insertLocation(loc: String, sId: Long) = {
        val l = new Location(loc, sId, None, None)
        EntityGraph.locations.insert(l)
    }

    def insertLocation(loc: String, sId: Long, lat: String, lng: String) = {
        val l = new Location(loc, sId, Some(lat), Some(lng))
        EntityGraph.locations.insert(l)
    }

    def insertConnection(propId: Long, govId: Long, depId: Long, strength: Int = 0) = {
        
    }



}

