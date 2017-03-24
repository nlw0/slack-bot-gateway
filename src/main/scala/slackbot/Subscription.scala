package slackbot

import akka.actor.ActorRef

case class Subscription(pattern: String, who: ActorRef) {
  lazy val r=pattern.r
}
