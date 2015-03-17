package cropsitedb.actors

import akka.actor.{Actor, Props, ActorLogging}
import akka.event.Logging

import java.io.File
import java.io.FileReader

import au.com.bytecode.opencsv.CSVReader

import play.api.db.DB
import anorm._
import cropsitedb.helpers.AnormHelper

import play.api.Play.current

class ProcessALINK extends Actor with ActorLogging {
  def receive = {
    case msg: Messages.ProcessFile => processing(msg)
    case _ => {}
  }

  def processing(msg: Messages.ProcessFile) = {
    val acmoFile = new File(msg.filePath)
    if (acmoFile.exists) {
      processALINK(msg.dsid, acmoFile.getName())
    } else {
      log.error("File not found")
    }
  }

  def processALINK(dsid: String, f: String) {
    log.info("ProcessALINK()")
    DB.withTransaction { implicit c =>
      log.info("In the DB")
      SQL("INSERT INTO alink_metadata (dsid, alink_file) VALUES ({dsid},{file})").on('dsid->dsid, 'file->f).execute()
    }
  }
}
