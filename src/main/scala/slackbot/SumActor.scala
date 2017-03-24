package slackbot

import akka.actor.Actor

class SumActor extends Actor {
  val slackGw = context.actorSelection("/user/slack-gateway")
  slackGw ! Subscription("sum [ \\d]*$", self)

  def receive = {
    case m: slack.models.Message =>
      println(s"Recebi!! $m")
      val numbers = m.text.split(" +").drop(1).map(_.toInt).toList
      slackGw ! PostToChannel(m.channel, s"<@${m.user}> your sum is ${numbers.sum}")
  }
}
