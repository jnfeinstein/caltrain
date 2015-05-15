package org.caltrain

import org.five11._
import org.joda.time.DateTime

object Main {
  implicit val token: Api.token = System.getenv("TOKEN")

  val api = new Api

  def main(args: Array[String]) = {

    implicit val caltrain = api.agencies.find{ _.name.toLowerCase == "caltrain" }.get

    while (true) {
      poll
      println("Sleeping - " + DateTime.now)
      Thread.sleep(180000)
    }
  }

  def poll(implicit agency: Agency) = {
    val now = DateTime.now

    println("Polling - " + now)

    agency.routes.map{ r: Route =>

      val departures = r.directions.map{ dir: Direction =>

        dir.stops.map{ s: Stop =>
          new DepartureModel(
            r.code.toString,
            dir.code.toString,
            s.code.toString,
            now,
            s.departures.sortBy{ _.time }.map{ _.time }
          )
        }

      }.flatten

      departures.foreach{ d =>
        println("Inserting " +
          Array(now,
            d.direction,
            d.stop,
            d.route,
            d.departures.mkString("/")).mkString(","))
      }

      DepartureRecordByTime.insertDepartures(departures)
      DepartureRecordByStop.insertDepartures(departures)
    }

  }

}
