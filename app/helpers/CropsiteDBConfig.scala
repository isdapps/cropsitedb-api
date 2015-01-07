package cropsitedb.helpers

import play.api.Play

object CropsiteDBConfig {
  val naviUrl = rewriteUrl(Play.current.configuration.getString("navi.baseurl").getOrElse("http://localhost:8082/navi/1"))

  def rewriteUrl(url: String):String = {
    if (url.endsWith("/")) {
      url.substring(0, url.length-1)
    } else {
      url
    }
  }
}


