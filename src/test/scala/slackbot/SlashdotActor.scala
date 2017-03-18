package slackbot

import akka.actor.Actor

import scala.concurrent.duration._
import scala.xml.XML

class SlashdotActor extends Actor {

  import context.dispatcher

  val gwActor = context.actorSelection("/user/slack-gateway")
  self ! "tick"

  def receive = {
    case "tick" =>
      context.system.scheduler.scheduleOnce(2.seconds, self, "tick")

      gwActor ! PostToChannel("C4L6FFMJQ", s"tick!")
      val sd = XML.load("http://rss.slashdot.org/Slashdot/slashdotMain")

      for (x <- (sd \\ "item").take(1)) {
        val title = (x \ "title").text
        val date = (x \ "date").text
        val msg = s"*********\n$date\n$title"
        println(msg)
        // gwActor ! PostToChannel("C4L6FFMJQ", msg)
      }
  }
}
