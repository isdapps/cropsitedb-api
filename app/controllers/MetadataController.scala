package cropsitedb.controllers

import anorm._
import play.api._
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc._

import cropsitedb.helpers.JsonHelper

import play.api.Play.current


object MetadataController extends Controller {
  case class MetadataRequest(key: String)
  implicit val MetadataRequestReads = Json.reads[MetadataRequest]

  def dumpMetadata = Action(parse.json) { implicit request =>
    val qReqRes = request.body.validate[MetadataRequest]
    qReqRes.fold(
      errors => {
        BadRequest(Json.obj("error" -> "Invalid query"))
      },
      qReq => {
        DB.withConnection { implicit c =>
          val securityQ = SQL("SELECT site FROM extern_hosts WHERE key={qKey}").on('qKey -> qReq.key).apply.headOption
          securityQ match {
            case None => BadRequest(Json.obj("error" -> "Invalid query"))
            case Some(siteRow) => {
              val site = siteRow[String]("site")
              
              Ok(site)
            }
          }
        }
      }
    )
  }
}






