package cropsitedb.controllers

import cropsitedb.helpers.{CropsiteDBConfig, JsonHelper}

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import scala.util.{Success, Failure}

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.db.DB
import anorm._
import cropsitedb.helpers.AnormHelper
import cropsitedb.helpers.GeoHashHelper

import play.Logger

import scala.concurrent.Future
import scala.collection.mutable.ListBuffer

import com.fasterxml.jackson.core.{JsonParser, JsonToken}

import org.agmip.ace.lookup.LookupCodes

object ExternalDataController extends Controller {
  def index = TODO
  def agtrials = Action.async { implicit request =>
    WS.url(CropsiteDBConfig.agtrialsUrl).get().map { res =>
      res.status match {
        case 200 => {
          val jp = JsonHelper.factory.createParser(res.body)
          val entries:ListBuffer[List[Tuple2[String,String]]] = ListBuffer()
          while(Option(jp.nextToken()).isDefined) {
            jp.getCurrentToken match {
              case JsonToken.START_OBJECT => {
                var entry += processAgtrials(jp, List(("dsid", "AgTrials"), ("api_source", "AgTrials")))
              }
              case _ => {}
            }
          }
          Ok("Done.")
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
            if (v.length > 0 && v != "null") {
              field match {
                case "id" => {
                  processAgtrials(p, Tuple2("eid","agtrials_"+v) :: collected)
                }
                case "pdate" | "hdate" => {
                  val d = v.replace("-", "").replaceAll("\\s+","")
                  if(d.length > 0)
                    processAgtrials(p, Tuple2(field,v.replace("-", "")) :: collected)
                  else
                    processAgtrials(p, collected)
                }
                case "crid" => {
                  val code = LookupCodes.modelLookupCode("agtrials", "crid", v)
                  processAgtrials(p, Tuple2(field, code) :: collected)
                }
                // Bad variable coming from AgTrials - CV 24-02-2015
                case "suite_id" => {
                  processAgtrials(p, Tuple2("suiteid", v) :: collected)
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

/*
 var lat:Option[String] = None
 var lng:Option[String] = None
 entry.foreach { t:Tuple2[String,String] =>
 t._1 match {
 case "fl_lat" => lat = Some(t._2)
 case "fl_long" => lng = Some(t._2)
 case _ => {}
 }
 }
 val point = GeoHashHelper.NaviLL(lat, lng)
 import scala.concurrent.ExecutionContext.Implicits.global
 val navi = WS.url(CropsiteDBConfig.naviUrl+"/point").post(Json.toJson(point)).map { res2 =>
 (res2.json).validate[GeoHashHelper.NaviPoint]
 }

 navi.onComplete {
 case Success(x) => x match {
 case n:JsSuccess[GeoHashHelper.NaviPoint] => {
 val navi = n.get
 navi.error match {
 case None => {
 if (navi.adm0.isDefined)
 entry :+ ("fl_loc_1", navi.adm0.get)
 if (navi.adm1.isDefined)
 entry :+ ("fl_loc_2", navi.adm1.get)
 if (navi.adm2.isDefined)
 entry :+ ("fl_loc_3", navi.adm2.get)
 entry :+ ("~fl_geohash", navi.geohash.get)
 SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(entry)+") VALUES ("+AnormHelper.valJoin(entry)+")").on(entry.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
 }
 case _ => {
 SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(entry)+") VALUES ("+AnormHelper.valJoin(entry)+")").on(entry.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
 }
 }
 }
 case _  => {
 SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(entry)+") VALUES ("+AnormHelper.valJoin(entry)+")").on(entry.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
 }
 }
 case Failure(y) => {
 SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(entry)+") VALUES ("+AnormHelper.valJoin(entry)+")").on(entry.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
 }
 }
 } catch {
 case ex: Exception => {}
 }
 */
