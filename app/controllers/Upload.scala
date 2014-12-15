package cropsitedb.controllers

import java.io.IOException

import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.FileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

import play.api._
import play.api.mvc._

import play.api.db.DB
import anorm._
import cropsitedb.helpers.{AnormHelper, CropsiteDBConfig}

import play.api.libs.json._
import play.api.libs.functional.syntax._

import cropsitedb.helpers.DatasetHelper

import play.api.Play.current

import play.api.Logger


object Upload extends Controller {
  def index = TODO


  /*
   * Create Dataset (/dataset/create)
   * Create a directory for this dataset to store files and add a reference to the database.
   */
  def  createDataset = Action(parse.json) { request =>
    val dscRequest = request.body.validate[DatasetHelper.CreateDatasetRequest]
    dscRequest.fold(
      errors => {
        BadRequest(Json.obj("error"->"Missing fields for dataset creation"))
      },
      dscReq => {
        val dsid = java.util.UUID.randomUUID().toString
        val destDir = dsPath(dsid, dscReq.freeze)
        Files.createDirectories(destDir)
        DB.withTransaction { implicit c =>
          SQL("INSERT INTO ace_datasets(dsid, title, email, frozen) VALUES ({d}, {t}, {e}, {f})").on("d"->dsid, "t"->dscReq.title, "e"->dscReq.email.toLowerCase, "f"->dscReq.freeze.getOrElse(false)).execute()
        }
        Ok(Json.obj("dsid"->dsid, "title"->dscReq.title, "dest"->destDir.toString))
      }
      )
  }

  def addToDataset(dsid: String) = Action(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { f =>
      val fileName    = f.filename
      val contentType = Files.probeContentType(f.ref.file.toPath)
      Logger.info("Uploading "+fileName+" - "+contentType)
      f.ref.clean()
    }
    Ok(Json.obj()).withHeaders("Access-Control-Allow-Origin" -> "*")
  }

  def removeFromDataset(dsid: String) = Action(parse.multipartFormData) { implicit request =>
    Ok(Json.obj())
  }


  def deleteDataset = Action(parse.json) { implicit request =>
    val dsdRequest = request.body.validate[DatasetHelper.DeleteDatasetRequest]
    dsdRequest.fold(
      errors => {
        BadRequest(Json.obj("error"->"Missing fields for dataset deletion"))
      },
      dsdReq => {
        DB.withTransaction { implicit c =>
          Logger.info("Checking the SQL")
          SQL("SELECT * FROM ace_datasets WHERE dsid={d} AND email={e}")
            .on("d"->dsdReq.dsid, "e"->dsdReq.email.toLowerCase)
            .apply().map{ r => 
                Logger.info("Attempting to delete..."+dsdReq.dsid)
                val p = dsPath(r[String]("dsid"), Option(r[Boolean]("frozen")))
                Logger.info("Deleting "+p+" from all")
                deleteDSFiles(p)
            }
            SQL("DELETE FROM ace_datasets WHERE dsid={d} AND email={e}")
              .on("d"->dsdReq.dsid, "e"->dsdReq.email.toLowerCase)
              .execute()
            Ok("Deleted "+dsdReq.dsid)
        }
      }
    )
  }

  def autodetectFileType() {}

  def dsPath(dsid: String, frozen: Option[Boolean]): Path  = {
    val dest = if(! (frozen.getOrElse(false))) "uploads" else "freezer"
    FileSystems.getDefault().getPath(dest, dsid)
  }

  def deleteDSFiles(path: Path) {
    if (Files.exists(path) && Files.isDirectory(path)) {
      Files.walkFileTree(path, new FileVisitor[Path] {
        def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE

        def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Logger.info("Deleteing "+file)
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE

        def postVisitDirectory(dir: Path, exc: IOException) = {
          Logger.info("Deleting "+dir)
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }
  }
}
