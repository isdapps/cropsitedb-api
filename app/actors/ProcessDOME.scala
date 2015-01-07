package cropsitedb.actors

import akka.actor.{Actor, Props, ActorLogging}
import akka.event.Logging

import java.io.File
import java.io.FileInputStream

import java.util.zip.GZIPInputStream

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}

import play.api.db.DB
import anorm._
import cropsitedb.helpers.{AnormHelper, GeoHashHelper,CropsiteDBConfig}

import play.api.Play.current

class ProcessDOME extends Actor with ActorLogging {

  val factory = new JsonFactory()

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
    val jp = factory.createParser(gis)
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
    val info = extractDomeInfo(p, List(("dsid", dsid), ("dome_id", name)))
    DB.withTransaction { implicit c =>
      SQL("INSERT INTO dome_metadata ("+AnormHelper.varJoin(info)+") VALUES ("+AnormHelper.valJoin(info)+")").on(info.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
    }
  }

  def extractDomeInfo(p: JsonParser, collected: List[Tuple2[String,String]]):List[Tuple2[String,String]] = {
    val t = p.nextToken
    t match {
      case JsonToken.END_OBJECT => collected
      case JsonToken.FIELD_NAME => {
        val field = p.getCurrentName()
        p.nextToken
        val value = Option(p.getText())
        value match {
          case None => extractDomeInfo(p, collected)
          case Some(v) => (if (v.length > 0) extractDomeInfo(p, Tuple2(field,v) :: collected) else extractDomeInfo(p, collected))
        }
      }
      case _ => extractDomeInfo(p, collected)
    }
  }
}
