package org.caltrain

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import scala.concurrent.duration._
import spray.can.Http

object Boot extends App {
  implicit val system = ActorSystem("caltrain")
  import system.dispatcher

  val analyticService = system.actorOf(Props[AnalyticServiceActor], "analytic-service")

  val apiService = system.actorOf(Props[ApiServiceActor], "api-service")
  IO(Http) ! Http.Bind(apiService, "localhost", port = 5000)

  val pollService = system.actorOf(Props[PollServiceActor], "poll-service")
  system.scheduler.schedule(0 milliseconds, 60000 milliseconds, pollService, "tick")

  sys.addShutdownHook {
    println("Stopping")
    system.shutdown
  }
}
