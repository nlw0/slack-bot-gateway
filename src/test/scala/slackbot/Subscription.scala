package slackbot

import akka.actor.ActorRef

import scala.util.matching.Regex

case class Subscription(pattern: Regex, who: ActorRef)
