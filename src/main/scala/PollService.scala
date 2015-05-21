package org.caltrain

import akka.actor._
import org.caltrain.db._
import org.five11._
import org.joda.time.DateTime

import Common._

class PollServiceActor extends Actor with PollService {
  def receive = {
    case _ => {
      context.actorFor("../analytic-service") ! poll()
    }
  }
}

trait PollService {
  def poll(): DateTime = {
    val now = DateTime.now.minuteOfHour().roundFloorCopy();

    println("Polling - " + now)

    caltrain.stops.foreach{ stop =>
      val departuresByRoute = stop.departures.groupBy{ _.route }

      val departureModels = stop.routes.map{ route =>
        val departuresByDirection: Map[Direction, Seq[Departure]] = departuresByRoute.get(route) match {
          case Some(departures) => departures.groupBy{ _.direction }
          case None => Map()
        }

        stop.directions.map{ direction =>
          val times = departuresByDirection.get(direction) match {
            case Some(departures) => departures.sortBy{ _.time }.map{ _.time }
            case None => Seq()
          }

          new DepartureModel(route.code, direction.code, stop.code, now, times)
        }

      }.flatten

      departureModels.foreach{ d =>
        println("Inserting " +
          Array(now,
            d.direction,
            d.stop,
            d.route,
            d.departures.mkString("/")).mkString(","))
      }
      DepartureRecordByTime.insertDepartures(departureModels)
      DepartureRecordByStop.insertDepartures(departureModels)
    }

    return now
  }
}
