package cropsitedb.actors

import akka.actor.{Actor, Props, ActorLogging}
import akka.event.Logging

import java.io.File
import java.io.FileInputStream

import play.api.db.DB
import anorm._
import cropsitedb.helpers.AnormHelper

import play.api.Play.current

class ProcessACMO extends Actor with ActorLogging {
	def receive = {
		case _ => {log.info("Actor Called")}
	}
}
