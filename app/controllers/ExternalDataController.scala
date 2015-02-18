package cropsitedb.controllers

import cropsitedb.helpers.{CropsiteDBConfig, JsonHelper}

import play.api._
import play.api.mvc._

import play.api.libs.ws._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.Logger

import scala.concurrent.Future

import com.fasterxml.jackson.core.{JsonParser, JsonToken}

import org.agmip.ace.lookup.LookupCodes

object ExternalDataController extends Controller {
  def index = TODO
  def agtrials = Action.async { implicit request =>
    WS.url(CropsiteDBConfig.agtrialsUrl).get().map { res =>
      res.status match {
        case 200 => {
          val jp = JsonHelper.factory.createParser(res.body)
          while(Option(jp.nextToken()).isDefined) {
            jp.getCurrentToken match {
              case JsonToken.START_OBJECT => {
                val entry = processAgtrials(jp, List(("dsid", "AgTrials"), ("api_source", "AgTrials")))
                Logger.info(""+entry)
              }
              case _ => {}
            }
          }
          Ok(res.body)
        }
        case _   => BadRequest("Error!")
      }
    }
  }


  def processAgtrials(p: JsonParser, collected: List[Tuple2[String,String]]): List[Tuple2[String,String]] = {
    val t = p.nextToken
    t match {
      case JsonToken.END_OBJECT => collected
      case JsonToken.FIELD_NAME => {
        val field = p.getCurrentName()
        p.nextToken
        val value = Option(p.getText())
        value match {
          case None => processAgtrials(p, collected)
          case Some(v) => {
            if (v.length > 0) {
              field match {
                case "id" => {
                  processAgtrials(p, Tuple2("eid","agtrials_"+v) :: collected)
                }
                case "pdate" | "hdate" => {
                  processAgtrials(p, Tuple2(field,v.replace("-", "")) :: collected)
                }
                case "crid" => {
                  val code = LookupCodes.modelLookupCode("agtrials", "crid", v)
                  processAgtrials(p, Tuple2(field, code) :: collected)
                }
                case _ => processAgtrials(p, Tuple2(field, v) :: collected)
              }
            } else {
              processAgtrials(p, collected)
            }
          }
        }
      }
      case _ => processAgtrials(p, collected)
    }
  }
}
