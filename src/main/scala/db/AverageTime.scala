package org.caltrain.db

import com.websudos.phantom.builder.Unspecified
import com.websudos.phantom.builder.query._
import com.websudos.phantom.dsl._
import org.five11._
import scala.concurrent.{ Await, Future => ScalaFuture }

case class AverageTimeModel (
  route: String,
  direction: String,
  fromStop: String,
  toStop: String,
  avgMinutes: Option[Int],
  samples: Int
)

sealed class AverageTimeTable extends CassandraTable[AverageTimeTable, AverageTimeModel] {
  object route extends StringColumn(this) with PrimaryKey[String]
  object direction extends StringColumn(this) with PrimaryKey[String]
  object fromStop extends StringColumn(this) with PrimaryKey[String]
  object toStop extends StringColumn(this) with PrimaryKey[String]
  object avgMinutes extends OptionalIntColumn(this)
  object samples extends IntColumn(this)

  def fromRow(row: Row): AverageTimeModel = {
    AverageTimeModel(
      route(row),
      direction(row),
      fromStop(row),
      toStop(row),
      avgMinutes(row),
      samples(row)
    )
  }
}

object AverageTimeRecord extends AverageTimeTable with CaltrainConnector {
  override lazy val tableName = "average_times"

  def insertQuery(model: AverageTimeModel): InsertQuery[AverageTimeTable, AverageTimeModel, Unspecified] = {
    insert.value(_.route, model.route)
      .value(_.direction, model.direction)
      .value(_.fromStop, model.fromStop)
      .value(_.toStop, model.toStop)
      .value(_.avgMinutes, model.avgMinutes)
      .value(_.samples, model.samples)
  }

  def insertModel(model: AverageTimeModel): ScalaFuture[ResultSet] = {
    insertQuery(model).future()
  }

  def insertModels(models: Seq[AverageTimeModel]): ScalaFuture[ResultSet] = {
    models.foldLeft(Batch.unlogged) { (op, model) =>
      op.add( insertQuery(model) )
    }.future()
  }

  def get(route: Route, direction: Direction): ScalaFuture[Seq[AverageTimeModel]] = {
    select.where(_.route eqs route.code).
      and(_.direction eqs direction.code).
      fetch()
  }
}
