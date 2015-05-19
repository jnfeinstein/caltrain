package org.caltrain

import org.five11._
import org.joda.time.DateTime

object Main {
  val api = new Api( System.getenv("TOKEN") )

  def main(args: Array[String]) = {

    implicit val caltrain = api.agencies.find{ _.name.toLowerCase == "caltrain" }.get

    while (true) {
      poll
      println("Sleeping - " + DateTime.now)
      Thread.sleep(60000)
    }
  }

  def poll(implicit agency: Agency) = {
    val now = DateTime.now

    println("Polling - " + now)

    agency.stops.par.foreach{ stop =>
      val departuresByRoute = stop.departures.groupBy{ _.route }

      val departureModels = stop.routes.map{ route =>
        val departuresByDirection: Map[Direction, Seq[Departure]] = departuresByRoute.get(route) match {
          case Some(departures) => departures.groupBy{ _.direction }
          case None => Map()
        }

        route.directions.map{ direction =>
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
  }

}
