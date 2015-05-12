import org.five11._
import org.joda.time.DateTime

object Main {
  implicit val token: Api.token = System.getenv("TOKEN")

  def main(args: Array[String]) = {

    val api = new Api

    val caltrain = api.agencies.find{ _.name.toLowerCase == "caltrain" }.get

    val time = DateTime.now

    caltrain.routes.foreach{ r: Route =>
      println(r)
      r.directions.foreach{ dir: Direction =>
        println(dir)
        dir.stops.foreach{ s: Stop =>
          println(s)
          s.departures.foreach{ d: Departure =>
            val departure = new DepartureModel(caltrain.toString, r.toString, dir.toString, s.toString, time, d.time)
            println("Inserting " + Array(caltrain, r, dir, s, time, d.time).mkString(","))
            DepartureRecord.insertDeparture(departure)
          }
        }
      }
    }
  }
}
