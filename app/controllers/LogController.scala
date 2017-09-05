package controllers

import javax.inject.{Inject, Singleton}

import io.circe.generic.auto._
import io.circe.syntax._
import models.Log
import play.api.libs.circe.Circe
import play.api.mvc._
import queries.CreateLog

@Singleton
class LogController @Inject()(val cc: ControllerComponents) extends AbstractController(cc) with Circe {
  import LogController._

  def show(id: Long) = Action {
    Log.findById(id).fold(NotFound(s"Not found id=${id}")) { log => Ok(log.asJson) }
  }

  def list() = Action {
    Ok(Log.findAll().asJson)
  }

  def create() = Action(circe.tolerantJson[CreateLog]) { req =>
    if(Log.create(req.body) > 0) Success else SQLError
  }

  def delete(id: Long) = Action {
    if(Log.deleteById(id) > 0) Success else SQLError
  }
}

object LogController {
  import Results._
  val Success = Ok("Success")
  val SQLError = InternalServerError("SQL Error")
}
