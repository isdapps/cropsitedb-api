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
import scala.collection.mutable.{MutableList, ListBuffer}


import com.fasterxml.jackson.core.{JsonParser, JsonToken}

import org.agmip.ace.lookup.LookupCodes

object ExternalDataController extends Controller {
  import scala.concurrent.ExecutionContext.Implicits.global
  def index = TODO
  def agtrials = Action.async { implicit request =>
    WS.url(CropsiteDBConfig.agtrialsUrl).get().map { res =>
      res.status match {
        case 200 => {
          val jp = JsonHelper.factory.createParser(res.body)
          // Parse all pulled entries
          val e:ListBuffer[Tuple3[Option[String], Option[String], List[Tuple2[String,String]]]] = ListBuffer()
          while(Option(jp.nextToken()).isDefined) {
            jp.getCurrentToken match {
              case JsonToken.START_OBJECT => {
                e += processAgtrials(jp, None, None, List(("dsid", "AgTrials"), ("api_source", "AgTrials")))
              }
              case _ => {}
            }
          }
          val points = e.toList.map { entry =>
            GeoHashHelper.NaviLL(entry._1, entry._2)
          }
          import scala.concurrent.ExecutionContext.Implicits.global
          val navi = WS.url(CropsiteDBConfig.naviUrl+"/points").post(Json.toJson(points)).map { res =>
            (res.json).validate[Seq[GeoHashHelper.NaviPoint]]
          }

          navi.onComplete {
            case Success(x) => x match {
              case n:JsSuccess[Seq[GeoHashHelper.NaviPoint]] => {
                val merged = (e.toList.map { _._3 }, n.get).zipped.map { (ex, navi) =>
                  navi.error match {
                    case None => {
                      val append:ListBuffer[Tuple2[String,String]] = ListBuffer()
                      if (navi.adm0.isDefined)
                        append += Tuple2("fl_loc_1", navi.adm0.get)
                      if (navi.adm1.isDefined)
                        append += Tuple2("fl_loc_2", navi.adm1.get)
                      if (navi.adm2.isDefined)
                        append += Tuple2("fl_loc_3", navi.adm2.get)
                      append += Tuple2("fl_geohash", navi.geohash.get)
                      Logger.debug(""+append.toList)
                      ex ::: append.toList
                    }
                    case _ => ex
                  }
                }
                Logger.debug("Final List: "+merged)
                writeAgtrials(merged)
              }
              case err:JsError  => {
                Logger.debug("Invalid JSON: "+err.errors)
                writeAgtrials(e.toList.map{_._3})
              }
            }
            case Failure(y) => {
              Logger.debug("Failed Navi Call: "+y)
              writeAgtrials(e.toList.map{_._3})
            }
          }
          Ok("Done.")
        }
        case _   => BadRequest("Error!")
      }
    }
  }


  @scala.annotation.tailrec
  def processAgtrials(p: JsonParser, lat: Option[String], lng: Option[String], collected: List[Tuple2[String,String]]): Tuple3[Option[String], Option[String], List[Tuple2[String,String]]] = {
    val t = p.nextToken
    t match {
      case JsonToken.END_OBJECT => {
        // Ask Navi for more information
        (lat, lng, collected)
      }
      case JsonToken.FIELD_NAME => {
        val field = p.getCurrentName()
        p.nextToken
        val value = Option(p.getText())
        value match {
          case None => processAgtrials(p, lat, lng, collected)
          case Some(v) => {
            if (v.length > 0 && v != "null") {
              field match {
                case "id" => {
                  processAgtrials(p, lat, lng, Tuple2("eid","agtrials_"+v) :: collected)
                }
                case "pdate" | "hdate" => {
                  val d = v.replace("-", "").replaceAll("\\s+","")
                  if(d.length > 0)
                    processAgtrials(p, lat, lng, Tuple2(field,v.replace("-", "")) :: collected)
                  else
                    processAgtrials(p, lat, lng, collected)
                }
                case "crid" => {
                  val code = LookupCodes.modelLookupCode("agtrials", "crid", v)
                  processAgtrials(p, lat, lng, Tuple2(field, code) :: collected)
                }
                // Bad variable coming from AgTrials - CV 24-02-2015
                case "suite_id" => {
                  processAgtrials(p, lat, lng, Tuple2("suiteid", v) :: collected)
                }
                // Handle lat/long for Navi
                case "fl_lat" => {
                  processAgtrials(p, Some(v), lng, Tuple2(field, v) :: collected )
                }
                case "fl_long" => {
                  processAgtrials(p, lat, Some(v), Tuple2(field, v) :: collected )
                }
                case _ => processAgtrials(p, lat, lng, Tuple2(field, v) :: collected)
              }
            } else {
              processAgtrials(p, lat, lng, collected)
            }
          }
        }
      }
      case _ => processAgtrials(p, lat, lng, collected)
    }
  }

  def writeAgtrials(entries: List[List[Tuple2[String,String]]]) {
    DB.withTransaction { implicit c =>
      entries.foreach { entry =>
        SQL("INSERT INTO ace_metadata ("+AnormHelper.varJoin(entry)+") VALUES ("+AnormHelper.valJoin(entry)+")").on(entry.map(AnormHelper.agmipToNamedParam(_)):_*).execute()
      }
    }
  }

  /*
   val naviReq = WS.url(CropsiteDBConfig.naviUrl+"/point").post(Json.toJson(GeoHashHelper.NaviLL(lat, lng))).map { res =>
   (res.json).validate[GeoHashHelper.NaviPoint] match {
   case naviRes:JsSuccess[GeoHashHelper.NaviPoint] => {
   var append:ListBuffer[Tuple2[String,String]] = ListBuffer()
   val navi = naviRes.get
   navi.error match {
   case None => {
   if (navi.adm0.isDefined)
   append :+ ("fl_loc_1", navi.adm0.get)
   if (navi.adm1.isDefined)
   append :+ ("fl_loc_2", navi.adm1.get)
   if (navi.adm2.isDefined)
   append :+ ("fl_loc_3", navi.adm2.get)
   append :+ ("~fl_geohash~", navi.geohash.get)
   collected ::: append.toList
   Logger.debug("NAVI SUCCESS!!!!!")
   }
   case _ => { Logger.debug("FAILED NAVI!!!")
   collected
   }
   }
   }
   case err: JsError => {
   Logger.debug("JSON FAILURE!!!!!")
   collected
   }
   }
   }
   naviReq.onComplete {
   case Success(item) => {
   println("SUCCESS!!!! "+item)
   //            writeAgtrials(item)
   }
   case Failure(t) => {
   println("FAILURE!!!! "+t)
   //            writeAgtrials(collected)
   }
   }
   List()

   */
}
