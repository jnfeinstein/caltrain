package org.caltrain

import akka.actor._
import org.caltrain.db._
import org.five11._
import org.joda.time.DateTime
import scala.util.{Success, Failure}

import Common._

class AnalyticServiceActor extends Actor with AnalyticService {
  def actorRefFactory = context
  def receive = {
    case time: DateTime => analyze(time)
  }
}

trait AnalyticService extends GenericService {
  implicit def executionContext = actorRefFactory.dispatcher

  def analyze(when: DateTime) = {
    caltrain.routes.flatMap{ route =>
      route.directions.map{ direction => (route, direction) }
    }.
    foreach{ case (route, direction) =>
      val chain = for {
        averageTimeModels <- AverageTimeRecord.get(route, direction)
        departureModels <- DepartureRecordByTime.get(direction, route, when)
      } yield (averageTimeModels, departureModels)

      chain.onSuccess { case (averageTimeModels, departureModels) =>
        val stops = route.stops.intersect(direction.stops).sorted{
          direction.code match {
            case "NB" => Ordering.by[Stop, String]( _.code ).reverse
            case _ => Ordering.by[Stop, String]( _.code )
          }
        }

        stops.zipWithIndex.
        flatMap{ case (toStop, i) =>
          stops.take(i).map{ fromStop => (fromStop, toStop) }
        }.
        map{ case (fromStop, toStop) =>
          val (avgMinutes, numSamples) = averageTimeModels.find{ m =>
            m.toStop == toStop.code && m.fromStop == fromStop.code
          } match {
            case Some(m) => (m.avgMinutes, m.samples)
            case None => (None, 0)
          }

          val fromStopDepartures = departureModels.find{ _.stop == fromStop.code } match {
            case Some(m) => m.departures
            case None => Seq()
          }

          val toStopDepartures = departureModels.find{ _.stop == toStop.code } match {
            case Some(m) => m.departures
            case None => Seq()
          }

          val minLength = math.min(fromStopDepartures.length, toStopDepartures.length)

          val newNumSamples = numSamples + minLength

          val newAvgMinutes = if (newNumSamples > 0) {
            val prevTotalMinutes = avgMinutes match {
              case Some(n) => n * numSamples
              case None => 0
            }

            val newTotalMinutes = fromStopDepartures.takeRight(minLength).
              zip( toStopDepartures.takeRight(minLength) ).
              map { case (x, y) => math.abs(x - y) }.
              fold(prevTotalMinutes){ _ + _ }

            Some(newTotalMinutes / newNumSamples)
          } else None

          new AverageTimeModel(
            route.code,
            direction.code,
            fromStop.code,
            toStop.code,
            newAvgMinutes,
            newNumSamples)
        }.
        foreach{ AverageTimeRecord.insertModel(_) }
      }
    }
  }
}
