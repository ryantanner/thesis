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
	
	def insertDocument(d: thesis.Document) = { 
		startDbSession()
        transaction {		
        val newDoc = new thesis.db.Document(d.filePath)
        EntityGraph.documents.insert(newDoc)
        println("Inserted document: " + d.filePath)
      }
    }

    def insertAlias(entityValue: String, representative: Boolean, filePath: String, masterId: Option[Long]): Long = {
      val docId = getDocumentId(filePath).first.id
      val newEnt = new Entity(entityValue, None, representative, masterId)
        EntityGraph.entities.insert(newEnt)
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


}

