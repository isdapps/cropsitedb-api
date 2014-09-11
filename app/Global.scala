import play.api.GlobalSettings
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, Props }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.mvc._
import cropsitedb.actors.ProcessACEB




class CorsFilter extends EssentialFilter {
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      next(requestHeader).map { result =>
        result.withHeaders("Access-Control-Allow-Origin" -> "*",
          "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS, PUT, DELETE",
          "Access-Control-Allow-Headers" -> "x-requested-with,content-type,Cache-Control,Pragma,Date")
      }
    }
  }
}

object Global extends GlobalSettings { //WithFilters(new CorsFilter) with GlobalSettings {
  override def onStart(application : play.api.Application) {
    val acebProc = Akka.system.actorOf(Props[ProcessACEB], name="process-aceb")
  }
}
