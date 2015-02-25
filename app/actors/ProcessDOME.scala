package cropsitedb.actors

import akka.actor.{Actor, Props, ActorLogging}
import akka.event.Logging

import java.io.File
import java.io.FileInputStream

import java.util.zip.GZIPInputStream

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}

import play.api.db.DB
import anorm._
import cropsitedb.helpers.{AnormHelper, GeoHashHelper,CropsiteDBConfig,JsonHelper}

import play.api.Play.current

class ProcessDOME extends Actor with ActorLogging {
  def receive = {
    case msg: Messages.ProcessFile => {
      processing(msg)
    }
  }

  def processing(msg: Messages.ProcessFile) = {
    val domeFile = new File(msg.filePath)
    if( domeFile.exists ) {
      processDome(msg.dsid, domeFile)
    } else {
      log.error("File not found")
    }
  }

  def processDome(dsid:String, f: File) {
    val fis = new FileInputStream(f)
    val gis = new GZIPInputStream(fis)
    val jp = JsonHelper.factory.createParser(gis)
    try {
      var level: Int = 0
      var currentDome: String = ""
      // Seek to info
      while(Option(jp.nextToken()).isDefined) {
        jp.getCurrentToken match {
          case JsonToken.START_OBJECT => {
            level = level + 1
            level match {
              case 1 => {
                jp.nextToken
                currentDome = jp.getCurrentName
                log.info("Found DOME: "+currentDome)
              }
              case _ => {
                jp.getCurrentName match {
                  case "info" => {
                    extractAndPostDomeInfo(jp, dsid, currentDome)
                  }
                  case _ => {}

                }
              }
            }
          }
          case JsonToken.END_OBJECT => {
            level = level - 1
          }
          case _ => {}
        }
      }
    } catch {
      case _ : Throwable => {}
    } finally {
      jp.close
      gis.close
      fis.close
    }
  }

  def extractAndPostDomeInfo(p: JsonParser, dsid:String, name: String) {
    val info = JsonHelper.toListTuple2(p, List(("dsid", dsid), ("dome_id", name)))
    DB.withTransaction { implicit c =>
      SQL("INSERT INTO dome_metadata ("+AnormHelper.varJoin(info)+") VALUES ("+AnormHelper.valJoin(info)+")").on(info.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
    }
  }
}
