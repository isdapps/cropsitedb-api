package cropsitedb.actors

import akka.actor.{Actor, Props}
import akka.event.Logging

import play.api.db.DB
import anorm._
import cropsitedb.helpers.{AnormHelper, GeoHashHelper,CropsiteDBConfig}

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import scala.util.{Success, Failure}

import java.io.File

import org.agmip.ace.{AceDataset, AceExperiment}
import org.agmip.ace.io.AceParser
import org.agmip.ace.util._

import scala.collection.JavaConversions._

import play.api.Play.current

class ProcessACEB extends Actor {
  def receive = {
    case msg: Messages.ProcessFile =>
      processing(msg)
  }

  def processing(msg: Messages.ProcessFile) = {
    val naviUrl = CropsiteDBConfig.naviUrl
    val acebFile = new File(msg.filePath)
    if(acebFile.canRead) {
      println(s"Importing "+msg.filePath+" file")
      val dataset = AceParser.parseACEB(acebFile)
      dataset.linkDataset
      val metadata = MetadataFilter.INSTANCE.getMetadata.toList
      val exps = dataset.getExperiments.toList
      val points = exps.map { ex =>
        GeoHashHelper.NaviLL(Option(ex.getValue("fl_lat")), Option(ex.getValue("fl_long")))
      }
      import scala.concurrent.ExecutionContext.Implicits.global
      val navi = WS.url(naviUrl+"/points").post(Json.toJson(points)).map { res =>
        (res.json).validate[Seq[GeoHashHelper.NaviPoint]]
      }

      navi.onComplete {
        case Success(x) => x match {
          case n:JsSuccess[Seq[GeoHashHelper.NaviPoint]] => {
            val merged = (exps, n.get).zipped.map { (ex, navi) =>
              navi.error match {
                case None => {
                  if (navi.adm0.isDefined)
                    ex.update("fl_loc_1", navi.adm0.get)
                  if (navi.adm1.isDefined)
                    ex.update("fl_loc_2", navi.adm1.get)
                  if (navi.adm2.isDefined)
                    ex.update("fl_loc_3", navi.adm2.get)
                  ex.update("~fl_geohash~", navi.geohash.get)
                  ex
                }
                case _ => ex
              }
            }
            extractAndPost(merged, metadata, msg.dsid)
          }
          case _  => extractAndPost(exps, metadata, msg.dsid)
        }
        case Failure(y) => extractAndPost(exps, metadata, msg.dsid)
      }
    }
  }

  private def extractAndPost(experiments: List[AceExperiment], metadata: List[String], dsid: String) = {
    val extracted = experiments.map(ex => {
      extractMetadata(ex, metadata, List(("dsid", dsid), ("fl_geohash", ex.getValueOr("~fl_geohash~", ""))))
    }).iterator
    println("Preparing to write ACEB Metadata to database")
    DB.withTransaction { implicit c =>
      extracted.foreach { ex =>
        SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(ex)+") VALUES ("+AnormHelper.valJoin(ex)+")").on(ex.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
      }
    }
  }


  @scala.annotation.tailrec
  private def extractMetadata(experiment: AceExperiment, metadata: List[String], collected: List[Tuple2[String,String]]):List[Tuple2[String,String]] = {
    val md = metadata.headOption
    if(md.isDefined) {
      val mdx = Option(AceFunctions.deepGetValue(experiment, md.get))
      if (mdx.isDefined) {
        extractMetadata(experiment, metadata.tail, Tuple2(md.get, if (md.get == "crid") mdx.get.toUpperCase else mdx.get) :: collected)
      } else {
        extractMetadata(experiment, metadata.tail, collected)
      }
    } else {
      collected
    }
  }
}

