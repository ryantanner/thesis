package thesis.db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.dsl._
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

class BaseEntity extends KeyedEntity[Long]	{
	
	val id:Long = 0
	
}

class Entity(val value: String,
			 val location: Option[String],
             val representative: Boolean,
             val master: Option[Long]) extends BaseEntity	{
				
    // master is the id in this same table of the representative alias of a dependent alias

	def this() = this("",Some(""),false, Some(0L))

	lazy val properties: OneToMany[Property] = EntityGraph.propertiesOfEntities.left(this)
	
	//lazy val connectedEntities = EntityGraph.propertiesOfEntities.left(this)
				
}
			
class Property(val value: String,
			val entityId: Long) extends KeyedEntity[CompositeKey2[String,Long]]	{

	def id = compositeKey(value,entityId)

	def this() = this("",0)
	
	lazy val entity: ManyToOne[Entity] = EntityGraph.propertiesOfEntities.right(this)

}

class PropertiesOfEntities(val entityId: Long,
							val propertyId: Long) extends KeyedEntity[CompositeKey2[Long,Long]]	{
								
								def id = compositeKey(propertyId,entityId)
								
								def this() = this(0,0)
								
}

class PropertyQuality(val propertyId: Long,
						val key: String,
						val quality: String,
						var strength: Int) extends KeyedEntity[CompositeKey2[Long,String]] {
							
							def id = compositeKey(propertyId,key)
							
							def this() = this(0,"","",0)
							
						}
			
class Connection(val propertyId: Long,
				val govEntityId: Long,
				val depEntityId: Long,
				var strength: Int) extends KeyedEntity[CompositeKey3[Long,Long,Long]]	{

	def id = compositeKey(propertyId,govEntityId,depEntityId)

	def this() = this(0,0,0,0)
					
}

class DocumentMatches(val entityId:Long,
					  val documentId:Long) extends KeyedEntity[CompositeKey2[Long,Long]]	{

	def id = compositeKey(entityId,documentId)

	def this() = this(0,0)

}				

class Document(val documentPath:String) extends BaseEntity	{
					
	def this() = this("")

}

class Sentence(val documentId: Long,
			    val sent: String) extends BaseEntity {
					
					
    lazy val locations: OneToMany[Location] = EntityGraph.sentenceToLocations.left(this)

				}

class Location(val loc: String,
                val sentenceId: Long,
                val lat: Option[String],
                val lng: Option[String]) extends KeyedEntity[CompositeKey2[String,Long]] {

    def id = compositeKey(loc, sentenceId)

    lazy val sentence: ManyToOne[Sentence] = EntityGraph.sentenceToLocations.right(this)

}



				

				
object EntityGraph extends Schema	{
	
	val entities = table[Entity]
	
	val properties = table[Property]
	
	//val propertiesOfEntities = table[PropertiesOfEntities]
	
	val propertiesOfEntities = oneToManyRelation(entities,properties).
		via((e,p) => (
			e.id === p.entityId
		))
	
/*	val connections = manyToManyRelation(entities,entities).
						via[Connection]((g,d,c) => (c.govEntityId === g.id,
													  c.depEntityId === d.id)) */
													
	val qualitiesOfProperties = table[PropertyQuality]
													
	val connections = table[Connection]
	
	val documents = table[Document]
	
	val sentences = table[Sentence]
	
	val sentencesOfDocuments = oneToManyRelation(documents,sentences).
								via((d,s) => (
									d.id === s.documentId
								))

	val entitiesFromDocs = manyToManyRelation(entities,documents).
							via[DocumentMatches]((e,d,dm) => (e.id === dm.entityId,
															  d.id === dm.documentId))
	
    val locations = table[Location]

    val sentenceToLocations = oneToManyRelation(sentences, locations).
                                via((s,l) => s.id === l.sentenceId)

	on(entities)(e => declare(
		e.id is(unique,autoIncremented),
        e.value is (indexed, dbType("text")),
        e.location is (indexed)
	))
	
	on(properties)(p => declare(
		p.value is(indexed, dbType("text"))
	))
	
	on(connections)(c => declare(
		c.strength defaultsTo(0)
	))
	
	on(qualitiesOfProperties)(q => declare(
		columns(q.key,q.quality) are(indexed)
	))

    on(locations)(l => declare(
      l.loc is(indexed),
      l.sentenceId is(indexed)
    ))

    on(sentences)(s => declare(
      s.sent is (dbType("text"))
    ))
	
	
}
