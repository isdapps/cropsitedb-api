import play.api.GlobalSettings
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, Props }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.mvc._
import cropsitedb.actors.{ProcessACEB, ProcessDOME, ProcessACMO}

object Global extends GlobalSettings {
  override def onStart(application : play.api.Application) {
    val acebProc = Akka.system.actorOf(Props[ProcessACEB], name="process-aceb")
	  val domeProc = Akka.system.actorOf(Props[ProcessDOME], name="process-dome")
	  val acmoProc = Akka.system.actorOf(Props[ProcessACMO], name="process-acmo")
  }
}
