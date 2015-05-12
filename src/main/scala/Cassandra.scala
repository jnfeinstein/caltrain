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
  departure: Int
)

sealed class DepartureRecord extends CassandraTable[DepartureRecord, DepartureModel] {

  object agencyName extends StringColumn(this)
  object routeName extends StringColumn(this)
  object directionName extends StringColumn(this)
  object stopName extends StringColumn(this)
  object timestamp extends DateTimeColumn(this)
  object departure extends IntColumn(this)

  def fromRow(row: Row): DepartureModel = {
    DepartureModel(
      agencyName(row),
      routeName(row),
      directionName(row),
      stopName(row),
      timestamp(row),
      departure(row)
    )
  }
}

trait DepartureConnector extends SimpleCassandraConnector {
  implicit val keySpace = KeySpace("caltrain")
  override val manager = new DefaultCassandraManager( Set(new InetSocketAddress("localhost", 9042) ) )
}

object DepartureRecord extends DepartureRecord with DepartureConnector {
  override lazy val tableName = "departure_samples"

  def insertDeparture(departure: DepartureModel): ScalaFuture[ResultSet] = {
    insert.value(_.agencyName, departure.agencyName)
      .value(_.routeName, departure.routeName)
      .value(_.directionName, departure.directionName)
      .value(_.stopName, departure.stopName)
      .value(_.timestamp, departure.timestamp)
      .value(_.departure, departure.departure)
      .future()
  }
}
