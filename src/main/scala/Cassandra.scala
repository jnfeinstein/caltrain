package org.caltrain

import com.websudos.phantom.connectors.{KeySpace, SimpleCassandraConnector, DefaultCassandraManager}
import com.websudos.phantom.dsl._
import com.websudos.phantom.Manager
import java.net.InetSocketAddress
import org.joda.time.DateTime
import scala.concurrent.{ Future => ScalaFuture }

case class DepartureModel (
  agencyName: String,
  routeName: String,
  directionName: String,
  stopName: String,
  timestamp: DateTime,
  departures: Seq[Int]
)

sealed class DepartureRecord extends CassandraTable[DepartureRecord, DepartureModel] {

  object agencyName extends StringColumn(this)
  object routeName extends StringColumn(this)
  object directionName extends StringColumn(this)
  object stopName extends StringColumn(this)
  object timestamp extends DateTimeColumn(this)
  object departures extends ListColumn[DepartureRecord, DepartureModel, Int](this)

  def fromRow(row: Row): DepartureModel = {
    DepartureModel(
      agencyName(row),
      routeName(row),
      directionName(row),
      stopName(row),
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

  def insertDeparture(model: DepartureModel): ScalaFuture[ResultSet] = {
    insert.value(_.agencyName, model.agencyName)
      .value(_.routeName, model.routeName)
      .value(_.directionName, model.directionName)
      .value(_.stopName, model.stopName)
      .value(_.timestamp, model.timestamp)
      .value(_.departures, model.departures.to[List])
      .future()
  }
}
