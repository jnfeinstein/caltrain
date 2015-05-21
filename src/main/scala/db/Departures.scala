package org.caltrain.db

import com.websudos.phantom.builder.Unspecified
import com.websudos.phantom.builder.query._
import com.websudos.phantom.dsl._
import org.five11._
import org.joda.time.DateTime
import scala.concurrent.{ Await, Future => ScalaFuture }

case class DepartureModel (
  route: String,
  direction: String,
  stop: String,
  timestamp: DateTime,
  departures: Seq[Int]
)

sealed class DepartureTable extends CassandraTable[DepartureTable, DepartureModel] {
  object route extends StringColumn(this) with PrimaryKey[String]
  object direction extends StringColumn(this) with PrimaryKey[String]
  object stop extends StringColumn(this) with PrimaryKey[String]
  object timestamp extends DateTimeColumn(this) with PrimaryKey[DateTime]
  object departures extends ListColumn[DepartureTable, DepartureModel, Int](this)

  def fromRow(row: Row): DepartureModel = {
    DepartureModel(
      route(row),
      direction(row),
      stop(row),
      timestamp(row),
      departures(row)
    )
  }
}

class DepartureRecord extends DepartureTable with CaltrainConnector {
  def insertDepartureQuery(model: DepartureModel): InsertQuery[DepartureTable, DepartureModel, Unspecified] = {
    insert.value(_.route, model.route)
      .value(_.direction, model.direction)
      .value(_.stop, model.stop)
      .value(_.timestamp, model.timestamp)
      .value(_.departures, model.departures.to[List])
  }

  def insertDeparture(model: DepartureModel): ScalaFuture[ResultSet] = {
    insertDepartureQuery(model).future()
  }

  def insertDepartures(models: Seq[DepartureModel]): ScalaFuture[ResultSet] = {
    models.foldLeft(Batch.unlogged) { (op, model) =>
      op.add( insertDepartureQuery(model) )
    }.future()
  }
}

object DepartureRecordByTime extends DepartureRecord {
  override lazy val tableName = "departure_samples_by_time"

  def get(
      direction: String,
      route: String,
      time: DateTime): ScalaFuture[Seq[DepartureModel]] = {
    select.where(_.direction eqs direction).
      and(_.route eqs route).
      and(_.timestamp gte time).
      fetch()
  }

  def get(
      direction: Direction,
      route: Route,
      time: DateTime): ScalaFuture[Seq[DepartureModel]] = {
    get(direction.code, route.code, time)
  }

  def getCurrent(
      direction: String,
      route: String): ScalaFuture[Seq[DepartureModel]] = {
    val time = DateTime.now.minusMinutes(1)
    get(direction, route, time)
  }
}

object DepartureRecordByStop extends DepartureRecord {
  override lazy val tableName = "departure_samples_by_stop"

  def getCurrent(stop: String, direction: String): ScalaFuture[Seq[DepartureModel]] = {
    val time = DateTime.now.minusMinutes(1)
    select.where(_.stop eqs stop).
      and(_.direction eqs direction).
      and(_.timestamp gte time).
      fetch()
  }
}
