package models

import java.util.{Date}

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import Utilities._

case class Entity(id: Pk[Long] = NotAssigned, location: Option[String], master: Option[Long], representative: Boolean, value: String, sentenceId: Long)
case class Sentence(id: Pk[Long] = NotAssigned, sent: String, documentId: Long)
case class Location(loc: String, sentenceId: Long, lng: Option[String], lat: Option[String])
case class Property(entityId: Long, value: String)
case class Document(id: Pk[Long], documentPath: String)
case class SentenceConnection(entityId: Long, entity: String, sentence: String, sentenceId: Long, sentenceCount: Long)
case class EntityKey(id: Pk[Long], value: String, rank: Long = 0)

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Entity {
  
  // -- Parsers
  
  /**
   * Parse an Entity from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("entity.id") ~
    get[Option[String]]("entity.location") ~
    get[Option[Long]]("entity.master") ~
    get[Boolean]("entity.representative") ~
    get[String]("entity.value") ~
    get[Long]("entity.sentenceId") map {
      case id~location~master~representative~value~sentenceId => Entity(id, location, master, representative, value, sentenceId)
    }
  }
  
  /**
   * Parse a (Entity,Sentence) from a ResultSet
   */
  val withSentence = Entity.simple ~ Sentence.simple map {
    case entity~sentence => (entity, sentence)
  }
  
  // -- Queries
  
  /**
   * Retrieve a computer from the id.
   */
  def findById(id: Long): Option[Entity] = {
    DB.withConnection { implicit connection =>
      SQL("select * from \"Entity\" where id = {id}").on('id -> id).as(Entity.simple.singleOpt)
    }
  }
  
   /*
   * Return a page of (Entity,Sentence).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(Entity,Sentence)] = {
    
    val offest = pageSize * page
    
    DB.withConnection { implicit connection =>
      
      val entities = SQL(
        """
          select * from Entity AS entity
          inner join Sentence AS sentence
          on entity.sentenceId = sentence.outputId
          where entity.value like {filter}
          order by {orderBy} 
          limit {pageSize} offset {offset}
        """
      ).on(
        'pageSize -> pageSize, 
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy
      ).as(Entity.withSentence *)

      val totalRows = SQL(
        """
          select count(*) from Entity 
          where entity.value like {filter}
        """
      ).on(
        'filter -> filter
      ).as(scalar[Long].single)

      Page(entities, page, offest, totalRows)
      
    }
    
  }


}

object EntityKey {
  
  // -- Parsers
  
  /**
   * Parse an Entity from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("id") ~
    get[String]("value") ~
    get[Long]("keyCount") map {
      case id~value~keyCount => EntityKey(id, value, keyCount)
    }
  }

  val value = {
    get[Pk[Long]]("id") ~
    get[String]("value") map {
      case id~value => EntityKey(id, value, 0)
    }
  }
  
  val keyValue = {
    get[Pk[Long]]("keyId") ~
    get[String]("value") map {
      case id~value => EntityKey(id, value, 0)
    }
  }
  
  // -- Queries
  
  /**
   * Retrieve a computer from the id.
   */
  def findById(id: Long): Option[EntityKey] = {
    DB.withConnection { implicit connection =>
      SQL("select * from EntityKey where id = {id}").on('id -> id).as(EntityKey.value.singleOpt)
    }
  }
  
   /*
   * Return a page of (EntityKey).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = ""): Page[EntityKey] = {
    
    val offest = pageSize * page
    
    DB.withConnection { implicit connection =>

      val entities = SQL(
          """
            CALL entity_keys({filter});
          """
      ).on(
        'pageSize -> pageSize,
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy
      ).as(EntityKey.simple *)

      val totalRows = SQL(filter match {
          case "" =>
            """
                select count(*) from EntityKey
            """
          case _ =>
            """
              select count(*) from EntityKey AS entityKey
              where match (entityKey.value) against ({filter})
            """
          }
      ).on(
        'filter -> filter
      ).as(scalar[Long].single)

      Page(entities, page, offest, totalRows)
      
    }
    
  }

  def locations(id: Int): List[(EntityKey, Location)] = {
        DB.withConnection { implicit connection =>

            val entities = SQL(
              """
                CALL ekey_to_location({id});
              """
            ).on(
                'id -> id
            ).as(EntityKey.withLocation *)

            entities.toList

        }
  }

  val withLocation = EntityKey.keyValue ~ Location.simple map {
    case entityKey~location=> (entityKey, location)
  }

}

object SentenceConnection {

  val simple =
    get[Long]("temp_entities.id") ~
    get[String]("temp_entities.value") ~
    get[String]("sentence.sent") ~
    get[Long]("sentence.id") ~
    get[Long]("sentenceCount") map {
      case entityId~entity~sentence~sentenceId~sentenceCount => SentenceConnection(entityId, entity, sentence, sentenceId, sentenceCount)
    }

  def sentenceConnectionsById(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, id: Long): Page[SentenceConnection] = {
    DB.withConnection { implicit connection =>

        val connections = SQL(
          """
            CALL entities_to_sentences({id});
          """
        ).on(
            'id -> id
        ).as(SentenceConnection.simple *)

      Page(connections, page, 0, 10)

    }
  }

  def sentenceConnections(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, input: String): Page[SentenceConnection] = {
    DB.withConnection { implicit connection =>

        val connections = SQL(
          """
            SET @stInput = {input}
            CALL keys_to_sents(@stInput)
          """
        ).on(
            'input -> input
        ).as(SentenceConnection.simple *)

        Page(connections, page, 0, 10)

    }
  }

  def entityKeyIntersection(id1: Long, id2: Long): Page[Sentence] = {
     DB.withConnection { implicit connection =>

        val connections1 = SQL(
          """
            CALL entities_to_sentences({id});
          """
        ).on(
            'id -> id1
        ).as(Sentence.simple *)

        val connections2 = SQL(
          """
            CALL entities_to_sentences({id});
          """
        ).on(
            'id -> id2
        ).as(Sentence.simple *)

        Page(connections1.intersect(connections2), 0, 0, 10)

    }
  }

}

object Sentence {

    val simple = {
        get[Pk[Long]]("sentence.id") ~
        get[String]("sentence.sent") ~
        get[Long]("sentence.documentId") map {
            case id~sent~documentId => Sentence(id, sent, documentId)
        }
    }

    val withEntities = Sentence.simple ~ Entity.simple map {
      case sentence~entity => (sentence, entity)
    }

    def connections(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, id: Long): Page[(Entity,Sentence)] = {
      DB.withConnection { implicit connection =>

        val connections = SQL(
          """
            select * from Sentence as sentence
            inner join Entity as entity
            on sentence.outputId = entity.sentenceId and sentence.documentId = entity.documentId+119
            where sentence.id = {id};
          """
        ).on(
            'id -> id
        ).as(Entity.withSentence *)

        Page(connections, page, 0, 10)
      }
    }

}

object Location {
  
  val simple = {
    get[String]("loc") ~
    get[Long]("sentenceId") ~
    get[Option[String]]("lng") ~
    get[Option[String]]("lat") map {
      case loc~sentId~lng~lat => Location(loc, sentId, lng, lat)
    }
  }

}

