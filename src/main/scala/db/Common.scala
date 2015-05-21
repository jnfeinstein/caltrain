package org.caltrain.db

import com.websudos.phantom.connectors.{KeySpace, SimpleCassandraConnector, DefaultCassandraManager}
import java.net.InetSocketAddress

trait CaltrainConnector extends SimpleCassandraConnector {
  implicit val keySpace = KeySpace("caltrain")
  override val manager = new DefaultCassandraManager( Set(new InetSocketAddress( System.getenv("CASSANDRA_HOST"), 9042) ) )
}
