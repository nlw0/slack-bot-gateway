package slackbot

import akka.actor.Actor

class SumActor extends Actor {
  val gwActor = context.actorSelection("/user/slack-gateway")
  gwActor ! Subscription("sum [ \\d]*$".r, self)

  def receive = {
    case m: slack.models.Message =>
      println(s"Recebi!! $m")
      val numbers = m.text.split(" +").drop(1).map(_.toInt).toList
      gwActor ! PostToChannel(m.channel, s"<@${m.user}> your sum is ${numbers.sum}")
  }
}
