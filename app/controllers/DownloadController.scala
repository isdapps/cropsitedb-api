package cropsitedb.controllers

import play.api._
import play.api.mvc._
import play.api.db.DB
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import anorm._
import anorm.SqlParser._

import java.io.File
import java.nio.file.{ Path, Paths, Files, FileSystems, FileSystem, StandardCopyOption }
import java.nio.file.attribute.{ PosixFilePermission, PosixFilePermissions }
import java.net.URI
import java.util.Date
import java.util.zip.GZIPOutputStream;
import java.text.SimpleDateFormat
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future

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

object DownloadController extends Controller {
  def tmpDir = Paths.get("tmp")

  def download = Action.async(parse.json) { implicit request =>
    val dlReqRes = request.body.validate[DownloadHelper.DownloadRequest]
    dlReqRes.fold(
      errors => {
        Future { BadRequest(Json.obj("error" -> "Invalid download request")) }
      },
      dlReq => {
        val dlReqId  = java.util.UUID.randomUUID.toString
        val fileTypes = fileTypeBuilder(dlReq.fileTypes)
        val dlPath = Paths.get("downloads",  dlReqId+".zip")
        withZipFilesystem(dlPath) { dl =>
          dlReq.downloads.foreach { dataset =>
            //ACEB Processing
            val destPath   = dl.getPath(fetchCleanDSName(dataset.dsid), "ace_data.aceb")
            val sourcePath = Paths.get("uploads", dlReq.downloads(0).dsid, "ace_data.aceb")
            buildACEB(sourcePath, destPath, tmpDir.resolve(dlReqId), dataset)
          }
        }

        val destUrl = routes.DownloadController.serve(dlReqId).absoluteURL(true)
        Future { Ok(Json.obj("url" -> destUrl)) }
      })
  }

  def serve(dlid: String) = Action { implicit request =>
    // By default, we should be downloading the .ZIP files
    val download = Paths.get("downloads", dlid+".zip")
    if (Files.isReadable(download.toAbsolutePath)) {
      Ok.sendFile(
        content = download.toAbsolutePath.toFile,
        fileName = _ => "agmip_download.zip"
      )
    } else {
      BadRequest("Missing file")
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

  private def buildACEB(source: Path, dest: Path, tmp: Path, data: DownloadHelper.DSIDRequest):Option[String] = {
    Files.isReadable(source) match {
      case false => Some("Missing the source file: "+source)
      case true  => { 
        Files.exists(dest) match {
          case true  => Some("Download file conflict. Please try again")
          case false => {
            Files.createDirectories(dest.getParent)
            Files.createDirectories(tmp)
            var sids:Set[String] = Set()
            var wids:Set[String] = Set()
            val destDS = new AceDataset()
            val sourceDS = AceParser.parseACEB(source.toAbsolutePath.toFile)
            sourceDS.getExperiments.toList.foreach { ex =>
              val eid = ex.getId(false)
              if (data.eids.contains(eid)) {
                destDS.addExperiment(ex.rebuildComponent())
                sids = sids + ex.getValueOr("sid", "INVALID")
                wids = wids + ex.getValueOr("wid", "INVALID")
              }
            }
            sourceDS.getSoils.toList.foreach { s =>
              if (sids.contains(s.getId)) {
                destDS.addSoil(s.rebuildComponent)
              }
            }
            sourceDS.getWeathers.toList.foreach { w =>
              if (wids.contains(w.getId(false))) {
                destDS.addWeather(w.rebuildComponent)
              }
            }
            destDS.linkDataset
            DB.withTransaction { implicit c =>
              destDS.getExperiments.toList.foreach { ex =>
                SQL("UPDATE ace_metadata SET download_count = download_count + 1 WHERE dsid={dsid} AND eid={eid}")
                  .on("dsid"->data.dsid, "eid"->ex.getId(true))
                  .executeUpdate()
              }
            }
            val tmpFile = Files.createTempFile(tmp, data.dsid, ".tmp").toAbsolutePath
            AceGenerator.generateACEB(tmpFile.toFile, destDS)
            Files.move(tmpFile, dest)
            None
          }
        }
      }
    }
  }

  private def fetchCleanDSName(id: String):String = {
    DB.withConnection { implicit c =>
      SQL("SELECT title FROM ace_datasets WHERE dsid = {dsid}").on("dsid"->id).as(scalar[Option[String]].single).getOrElse(id)
    }
  }

  private def withZipFilesystem(zipFile: Path, overwrite: Boolean = true)(f: FileSystem => Unit) {
    if (overwrite) Files deleteIfExists zipFile
    val env = Map("create" -> "true").asJava
  val uri = new URI("jar", zipFile.toUri().toString(), null)

  val system = FileSystems.newFileSystem(uri, env)
  try {
    f(system)
    } finally {
      system.close()
    }
  }
}
