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

    agency.routes.foreach{ r: Route =>

      r.directions.foreach{ dir: Direction =>

        dir.stops.foreach{ s: Stop =>
          val departures = s.departures.sortBy{ _.time }.map{ _.time }
          if ( !departures.isEmpty ) {
            val model = new DepartureModel(
              agency.toString,
              r.toString,
              dir.toString,
              s.toString,
              now,
              departures
            )
            println("Inserting " + Array(now, agency, r, dir, s, departures.mkString("/")).mkString(","))
            DepartureRecord.insertDeparture(model)
          }
        }

      }

    }

  }

}
