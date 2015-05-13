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

    val departures = agency.routes.map{ r: Route =>

      r.directions.map{ dir: Direction =>

        dir.stops.map{ s: Stop =>
          new DepartureModel(
            agency.toString,
            r.code.toString,
            dir.code.toString,
            s.code.toString,
            now,
            s.departures.sortBy{ _.time }.map{ _.time }
          )
        }

      }.flatten

    }.flatten

    departures.foreach{ d =>
      println("Inserting " +
        Array(now,
          d.agencyName,
          d.directionCode,
          d.stopCode,
          d.routeCode,
          d.departures.mkString("/")).mkString(","))
    }

    DepartureRecord.insertDepartures(departures)

  }

}
