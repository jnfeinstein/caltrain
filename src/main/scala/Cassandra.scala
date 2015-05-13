package org.caltrain

import com.websudos.phantom.connectors.{KeySpace, SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.builder.Unspecified
import com.websudos.phantom.builder.query._
import com.websudos.phantom.dsl._
import com.websudos.phantom.Manager
import java.net.InetSocketAddress
import org.joda.time.DateTime
import scala.concurrent.{ Future => ScalaFuture }

case class DepartureModel (
  agencyName: String,
  routeCode: String,
  directionCode: String,
  stopCode: String,
  timestamp: DateTime,
  departures: Seq[Int]
)

sealed class DepartureRecord extends CassandraTable[DepartureRecord, DepartureModel] {

  object agencyName extends StringColumn(this)
  object routeCode extends StringColumn(this)
  object directionCode extends StringColumn(this)
  object stopCode extends StringColumn(this)
  object timestamp extends DateTimeColumn(this)
  object departures extends ListColumn[DepartureRecord, DepartureModel, Int](this)

  def fromRow(row: Row): DepartureModel = {
    DepartureModel(
      agencyName(row),
      routeCode(row),
      directionCode(row),
      stopCode(row),
      timestamp(row),
      departures(row)
    )
  }
}

trait DepartureConnector extends SimpleCassandraConnector {
  implicit val keySpace = KeySpace("caltrain")
  override val manager = new DefaultCassandraManager( Set(new InetSocketAddress( System.getenv("CASSANDRA_HOST"), 9042) ) )
}

object DepartureRecord extends DepartureRecord with DepartureConnector {
  override lazy val tableName = "departure_samples"

  def insertDepartureQuery(model: DepartureModel): InsertQuery[DepartureRecord, DepartureModel, Unspecified] = {
    insert.value(_.agencyName, model.agencyName.toLowerCase)
      .value(_.routeCode, model.routeCode.toLowerCase)
      .value(_.directionCode, model.directionCode.toLowerCase)
      .value(_.stopCode, model.stopCode.toLowerCase)
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
