package cropsitedb.actors

import akka.actor.{Actor, Props, ActorLogging}
import akka.event.Logging

import java.io.File
import java.io.FileInputStream

import java.util.zip.GZIPInputStream

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}

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
    } else {
      log.info("File does not exist")
    }
	}

	def processDome(f: File) {
		val fis = new FileInputStream(f)
		val gis = new GZIPInputStream(fis)
		val jp = factory.createParser(gis)
		try {
		} catch {
			case _ : Throwable => {}
		} finally {
			jp.close
			gis.close
			fis.close
		}
	}
}
