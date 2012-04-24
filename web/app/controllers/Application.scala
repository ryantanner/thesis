package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import play.api.libs.json._

import anorm._

import views._
import models._

/**
 * Manage a database of computers
 */
object Application extends Controller { 
  
  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list(0, 2, ""))

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action { Home }
  
  /**
   * Display the paginated list of computers.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on computer names
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(html.entityKeys(
      EntityKey.list(page = page, orderBy = orderBy, filter = filter),
      orderBy, filter
    ))
  }

  def sentenceConnections(page: Int, orderBy: Int, id: Long) = Action { implicit request =>
    Ok(html.sentenceConnections(
      SentenceConnection.sentenceConnectionsById(id = id), orderBy, id
    )
    )
  }

  def entityIntersection(id1: Long, id2: Long) = Action { implicit request =>
    Ok(html.sentences(
      SentenceConnection.entityKeyIntersection(id1, id2), 0, ""
    )
    )
  }

  def sentToEnts(page: Int, orderBy: Int, id: Long) = Action { implicit request =>
    Ok(html.list(
      Sentence.connections(page = page, orderBy = orderBy, id = id),
      orderBy, id.toString
    ))
  }

  def locations(id: Int) = Action { implicit request =>
    Ok(html.map(EntityKey.findById(id).get))
  }

  def entityKeyToLocation(id: Int) = Action { implicit request =>
    Ok(Json.toJson(
      EntityKey.locations(id) map { case (entity, location) =>
        Map[String,String](
            "entityId" -> entity.id.toString,
            "entityValue" -> entity.value,
            "location" -> location.loc,
            "lng" -> location.lng.get,
            "lat" -> location.lat.get,
            "sentence" -> location.sentenceId.toString
        )
      }
    ))
  }


}
            
