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
    val time = DateTime.now

    println("Polling - " + time)

    agency.routes.foreach{ r: Route =>
      println(r)

      r.directions.foreach{ dir: Direction =>
        println(dir)

        dir.stops.foreach{ s: Stop =>
          println(s)

          s.departures.foreach{ d: Departure =>
            val departure = new DepartureModel(agency.toString, r.toString, dir.toString, s.toString, time, d.time)

            println("Inserting " + Array(agency, r, dir, s, time, d.time).mkString(","))
            DepartureRecord.insertDeparture(departure)
          }
        }
      }
    }
  }
}
