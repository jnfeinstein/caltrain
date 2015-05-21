package org.caltrain

import akka.actor._
import scala.util.{Success, Failure}
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import spray.routing.Directives._
import spray.routing.HttpService

import Common.caltrain
import JsonProtocol._

class ApiServiceActor extends Actor with ApiService {
  def actorRefFactory = context
  def receive = runRoute(route)
}

trait ApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher

  val route = {
    path("routes") {
      respondWithMediaType(`application/json`) {
        val map = caltrain.routes.map{ r => r.code -> r.name }.toMap
        complete(map)
      }
    } ~
    path("directions") {
      respondWithMediaType(`application/json`) {
        val map = caltrain.directions.map{ d => d.code -> d.name }.toMap
        complete(map)
      }
    } ~
    path("stops") {
      parameters('direction, 'route) { (dirCode, routeCode) =>
        respondWithMediaType(`application/json`) {
          val route = caltrain.routes.find{ r => r.code == routeCode } match {
            case Some(r) => r
            case None => null
          }
          val direction = caltrain.directions.find{ d => d.code == dirCode } match {
            case Some(d) => d
            case None => null
          }
          if (route == null || direction == null) {
            reject()
          } else {
            val stops = caltrain.stops.filter{ s =>
              s.routes.contains(route) && s.directions.contains(direction)
            }
            val map = stops.map{ stop => stop.code -> stop.name }.toMap
            complete(map)
          }
        }
      }
    } ~
    path("times") {
      parameters('direction, 'route) { (dirCode, routeCode) =>
        onComplete( DepartureRecordByTime.getCurrent(dirCode, routeCode) ) {
          case Success(departureModels) => {
            respondWithMediaType(`application/json`) {
              val map = departureModels.map{ m => m.stop -> m.departures }.toMap
              complete(map)
            }
          }
          case _ => reject()
        }
      } ~
      parameters('stop, 'direction) { (stopCode, dirCode) =>
        onComplete( DepartureRecordByStop.getCurrent(stopCode, dirCode) ) {
          case Success(departureModels) => {
            respondWithMediaType(`application/json`) {
              val map = departureModels.map{ m => m.route -> m.departures }.toMap
              complete(map)
            }
          }
          case _ => reject()
        }
      }
    } ~
    pathSingleSlash {
      getFromFile("client/index.html")
    } ~
    getFromDirectory("client")
  }
}
