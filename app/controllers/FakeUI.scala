package fakeui.controllers

import play.api._
import play.api.mvc._

import play.api.db.DB
import play.api.libs.json._
import anorm._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws._
import scala.concurrent.Future

import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

import views.html.FakeUI._

object FakeUI extends Controller {
  val baseUrl = "http://if-abe-oven:9000/cropsitedb/2/"

  def start = Action.async {
    
    val cropWS = WS.url(baseUrl+"cache/crop").withHeaders("Accept" -> "application/json")
    cropWS.get().map {
      res => {
        val crops = (res.json).as[Seq[String]]
        Ok(views.html.FakeUI.start(crops))
      }
    }
  }

  def listByCrop(crid: String)  = Action.async {
    val listWS = WS.url(baseUrl+"query?crid="+crid).withHeaders("Accept" -> "application/json")
    listWS.get().map {
      res => {
        val metadata = (res.json).as[Seq[Map[String,String]]]
        Ok(views.html.FakeUI.crops(metadata))
      }
    }
  }
  def getDownloadURL = Action.async(parse.tolerantFormUrlEncoded) { implicit request =>
    val query = request.body.get("selection").getOrElse(Seq("[]")).head
    WS.url(baseUrl+"download").withHeaders("Accept" -> "application/json").withMethod("POST").withBody(Json.parse(query)).stream().map {
      case (response, body) =>

        // Check that the response was successful
        if (response.status == 200) {

          // Get the content type
          val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
            .getOrElse("application/octet-stream")

          // If there's a content length, send that, otherwise return the body chunked
          println(s"Content Type: $contentType")
          response.headers.get("Content-Length") match {
            case Some(Seq(length)) =>
              Ok.feed(body).as(contentType).withHeaders("Content-Length" -> length, "Content-Disposition" -> "attachment; filename=download.aceb")
            case _ =>
              Ok.chunked(body).as(contentType).withHeaders("Content-Disposition" -> "attachment; filename=download.aceb")
          }
        } else {
          BadGateway
        }
    }
  }

  def upload = TODO

}
