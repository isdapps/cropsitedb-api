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
		log.info("FIRING!!!")
		val domeFile = new File(msg.filePath)
		if( domeFile.exists ) {
			log.info("File exists")
			processDome(domeFile)
		} else {
			log.info("File does not exist")
		}
	}

	def processDome(f: File) {
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
									case "info" => parseDomeInfo(jp, currentDome)
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

	def parseDomeInfo(p: JsonParser, name: String) {
		log.info("Launching DOME Info Parser for "+name)
		log.info("STRING: "+p.getValueAsString())
	}
}
