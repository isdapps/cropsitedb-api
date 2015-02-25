package cropsitedb.helpers

import play.api.Play

object CropsiteDBConfig {
  val naviUrl = rewriteUrl(Play.current.configuration.getString("navi.baseurl").getOrElse("http://localhost:8082/navi/1"))
  val agtrialsUrl = rewriteUrl(Play.current.configuration.getString("extern.agtrials.url").getOrElse("http://localhost:8083/api/agtrials"))

  def rewriteUrl(url: String):String = {
    if (url.endsWith("/")) {
      url.substring(0, url.length-1)
    } else {
      url
    }
  }
}


