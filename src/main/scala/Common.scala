package org.caltrain

import akka.actor._
import org.caltrain.db._
import org.five11._
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}
import spray.json._

trait GenericService {
  implicit def actorRefFactory: ActorRefFactory
}

object Common {
  val api = new Api( System.getenv("TOKEN") )
  val caltrain = api.agencies.find{ _.name.toLowerCase == "caltrain" }.get
}

object JsonProtocol extends DefaultJsonProtocol {
  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

    val formatter = ISODateTimeFormat.basicDateTimeNoMillis

    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): DateTime = json match {
      case JsString(s) => try {
        formatter.parseDateTime(s)
      }
      catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }

  implicit val departureModelFormat = jsonFormat5(DepartureModel)
}
