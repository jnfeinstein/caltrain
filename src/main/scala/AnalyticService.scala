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
        foreach{ case (fromStop, toStop) =>
          val (avgMinutes, numSamples) = averageTimeModels.find{ m =>
            m.toStop == toStop.code && m.fromStop == fromStop.code
          } match {
            case Some(m) => (m.avgMinutes, m.samples)
            case None => (None, 0)
          }

          val fromSamples = departureModels.find{ _.stop == fromStop.code } match {
            case Some(m) => m.departures
            case None => Seq()
          }

          val toSamples = departureModels.find{ _.stop == toStop.code } match {
            case Some(m) => m.departures
            case None => Seq()
          }

          val fromLength = fromSamples.length
          val toLength = toSamples.length

          val (validFromSamples, validToSamples) =
            if (fromLength < toLength) {
              // There is another train above fromStop
              ( fromSamples, toSamples.takeRight(fromLength) )
            } else if (fromLength > toLength) {
              // There is a train too far away from toStop
              ( fromSamples.take(toLength), toSamples)
            } else {
              (fromSamples, toSamples)
            }

          val diffs = validFromSamples.zip(validToSamples).
            map { case (from, to) =>
              if (from < to) {
                Some(to - from)
              } else None
            }.flatten

          if (diffs.length > 0) {
            val prevTotalMinutes = avgMinutes match {
              case Some(n) => n * numSamples
              case None => 0
            }
            val newNumSamples = numSamples + diffs.length
            val newAvgMinutes = diffs.fold(prevTotalMinutes){ _ + _ } / newNumSamples

            val model = new AverageTimeModel(
                              route.code,
                              direction.code,
                              fromStop.code,
                              toStop.code,
                              Some(newAvgMinutes),
                              newNumSamples)

            AverageTimeRecord.insertModel(model)
          }
        }
      }
    }
  }
}
