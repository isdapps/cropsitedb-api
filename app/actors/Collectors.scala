package cropsitedb.actors;

import akka.actor.{Actor, Props}
//import akka.event.Logging

import java.io.File
import scala.collection.JavaConversions._

import play.api.db.DB
import anorm._
import anorm.SqlParser._

import play.api.Play.current

import org.agmip.ace.io._
import org.agmip.ace.AceDataset

class ACECollector(var downloadId: String) {
  def datasetAllowed(dsid: String): Boolean = {
    var allowed = false
    DB.withTransaction { implicit c =>
      val entry = SQL("SELECT frozen FROM ace_datasets WHERE dsid={dsid}").on("dsid" -> dsid).as(scalar[Boolean].singleOpt)
      allowed = !entry.getOrElse(true)
    }
    allowed
  }

  def collectExp(dsid: String, eids: Seq[String]) = {
    if (datasetAllowed(dsid)) {
      val dest = new AceDataset
      val df = new java.io.File("downloads"+File.separator+downloadId+File.separator+dsid+".aceb")
      val sf = new java.io.File("uploads"+File.separator+dsid+".aceb")
      if(sf.canRead) {
        var sids:Set[String] = Set()
        var wids:Set[String] = Set()
        val source = AceParser.parseACEB(sf)
        source.linkDataset
        source.getExperiments.toList.foreach { ex =>
          val eid = ex.getId
          if (eids.contains(eid)) {
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
        AceGenerator.generateACEB(df, dest);
      }
    }
  }
}
