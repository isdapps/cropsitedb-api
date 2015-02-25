package cropsitedb.controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import play.api.db.DB
import play.api.libs.json._
import play.api.libs.functional.syntax._
import anorm._

import play.api.libs.ws._
import scala.concurrent.Future

import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.JavaConversions._

import play.api.libs.concurrent.Akka

import cropsitedb.actors.{Messages,ProcessACEB}
import cropsitedb.helpers._

import org.agmip.ace.AceDataset;
import org.agmip.ace.AceExperiment;
import org.agmip.ace.AceWeather;
import org.agmip.ace.AceSoil;
import org.agmip.ace.io._
import org.agmip.ace.util._
import org.agmip.ace.lookup.LookupCodes

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger

object Application extends Controller {

  case class CropCache(crid: String, name: String)

  val df = new SimpleDateFormat("yyyyMMdd")

  def headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization, Referer, Host, DNT, Accept-Encoding, Accept-Language, User-Agent, Cache-Control, X-Requested-With",
    "Access-Control-Allow-Credentials" -> "true"
  )

  def rootOptions = options("/")
  def options(url: String) = Action { request =>
    NoContent.withHeaders(headers : _*)
  }


  // Root index - Documentation
  def index  = TODO

  def geohashQuery = Action(parse.json) { implicit request =>
    val qReqRes = request.body.validate[GeoHashHelper.GeoHashList]
    qReqRes.fold(
      errors => {
        BadRequest(Json.obj("error" -> "Invalid query"))
      },
      qReq => {
        DB.withConnection { implicit c =>
          val e = qReq.crid match {
            case Some(cr:String) => SQL("SELECT * FROM ace_metadata WHERE fl_geohash IN ({geohashes}) AND crid={crid} ORDER BY (dsid,crid,exname)").on('geohashes -> qReq.locations, 'crid -> cr).apply
            case _       => SQL("SELECT * FROM ace_metadata WHERE fl_geohash IN ({geohashes}) ORDER BY (dsid,crid,exname)").on('geohashes -> qReq.locations).apply
          }
          Ok(JsonHelper.structureQueryOutput(e))
        }
      }
    )
  }

  // Generic GET Search
  def query = Action { implicit request =>
    val params = request.queryString
    DB.withConnection { implicit c =>
      val e = SQL("SELECT * FROM ace_metadata WHERE "+AnormHelper.dynIntersectBuilder(params)).
        on(params.map(AnormHelper.dynQueryToNamedParam(_)).toSeq:_*)
      //Logger.info(e.toString)
      Ok(JsonHelper.structureQueryOutput(e.apply))
    }
  }

  // Download POST
  def download = Action.async(parse.json) { implicit request =>
    val dlReqRes = request.body.validate[DownloadHelper.DownloadRequest]
    dlReqRes.fold(
      errors => {
        Future { BadRequest(Json.obj("error" -> "Invalid download request")) }
      },
      dlReq => {
        val dlReqId  = java.util.UUID.randomUUID.toString
        val fileTypes = fileTypeBuilder(dlReq.fileTypes)
        //val collector = new ACECollector(dlReqId)
        if(dlReq.downloads.size() == 1 && fileTypes.size() == 1) {
          Future { BadRequest(Json.obj("error" -> "Running single file")) }
        } else {
          // Let's build a zip
          Future { BadRequest(Json.obj("error" -> fileTypes)) }
        }
      }
        /*
         dlReq.downloads.foreach { download =>
         val sourceFile = new File("./uploads/"+download.dsid+".aceb")
         var sids:Set[String] = Set()
         var wids:Set[String] = Set()
         if (sourceFile.canRead) {
         val source = AceParser.parseACEB(sourceFile)
         source.linkDataset
         source.getExperiments.toList.foreach { ex =>
         val eid = ex.getId
         if (download.eids.contains(eid)) {
         dest.addExperiment(ex.rebuildComponent())
         sids = sids + ex.getSoil.getId
         wids = wids + ex.getWeather.getId
         }
         }
         source.getSoils.toList.foreach { s =>
         if (sids.contains(s.getId)) {
         dest.addSoil(s.rebuildComponent)
         }
         }
         source.getWeathers.toList.foreach { w =>
         if (wids.contains(w.getId)) {
         dest.addWeather(w.rebuildComponent)
         }
         }
         dest.linkDataset
         } else {
         Future { BadRequest(Json.obj("error" -> "Missing dataset file")) }
         }
         }
         }
         Logger.info("Generating ACEB");
         AceGenerator.generateACEB(destFile, dest)
         val destUrl = routes.Application.serve(dlReqId).absoluteURL(true)
         Future { Ok(Json.obj("url" -> destUrl)) }
         }*/
    )
  }

  // Download GET
  def serve(dlid: String) = Action { implicit request =>
    // By default, we should be downloading the .ZIP files
    val dlName = dlid+".aceb"
    val download = new File("./downloads/"+dlName)
    if (download.canRead) {
      val dlContent: Enumerator[Array[Byte]] = Enumerator.fromFile(download)
      Result(
        header = ResponseHeader(200, Map(CONTENT_LENGTH -> download.length.toString,
          CONTENT_TYPE -> "application/x-gzip")),
        body = dlContent
      )
    } else {
      BadRequest("Missing file")
    }
  }

  implicit val cropCacheWrites: Writes[CropCache] = (
    (JsPath \ "crid").write[String] and
      (JsPath \ "name").write[String]
  )(unlift(CropCache.unapply))

  def cropCache = Action {
    DB.withConnection { implicit c =>
      val q = SQL("SELECT DISTINCT crid FROM ace_metadata ORDER BY crid")
      val crops = q().map { r => {
        val crid = r[Option[String]]("crid")
        CropCache(crid.get, LookupCodes.lookupCode("crid", crid.get, "cn"))
      }}.toList

      Ok(Json.toJson(crops))
    }
  }

  def locationCache = Action { implicit request =>
    val params = request.getQueryString("crid")
    DB.withConnection { implicit c =>
      val q = params match {
        case Some(p: String) => SQL("SELECT fl_lat, fl_long, fl_geohash, count(*) AS count FROM ace_metadata  WHERE crid={crid} GROUP BY fl_lat, fl_long, fl_geohash").on("crid"->p.toUpperCase)
        case _               => SQL("SELECT fl_lat, fl_long, fl_geohash, count(*) AS count FROM ace_metadata GROUP BY fl_lat, fl_long, fl_geohash")
      }
      val loc = q().map { r => {
        val lat = r[Option[String]]("fl_lat")
        val lng = r[Option[String]]("fl_long")
        val gh  = r[Option[String]]("fl_geohash")
        val c   = r[Long]("count")


        if(gh.isDefined && gh.get == "") {
          GeoJsonHelper.GeoJsonPoint(Some("0.0"), Some("0.0"), c, Some(""))
        } else {
          GeoJsonHelper.GeoJsonPoint(lat.map{l:String => if(l.endsWith(".")) l.dropRight(1) else l},lng.map{l:String => if(l.endsWith(".")) l.dropRight(1) else l}, c, gh)
        }
      }}.toList

      Ok(GeoJsonHelper.buildLocations(loc))
    }
  }

  def fileTypeBuilder(ft: Int): List[String] = {
    fileTypeBuilderL(ft, 1, List())
  }

  def fileTypeBuilderL(ft: Int, test: Int, l: List[String]): List[String] = {
    test match {
      case 1 =>
        if ((ft & test) != 0) fileTypeBuilderL(ft, 2, l :+ "ACEB") else fileTypeBuilderL(ft, 2, l)
      case 2 =>
        if ((ft & test) != 0) fileTypeBuilderL(ft, 4, l :+ "DOME") else fileTypeBuilderL(ft, 4, l)
      case 4 =>
        if ((ft & test) != 0) l :+ "ACMO" else l
      case _ =>
        l
    }
  }
}
