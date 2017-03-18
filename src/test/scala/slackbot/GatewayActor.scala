package slackbot

import akka.actor.Actor
import slack.rtm.SlackRtmClient

class GatewayActor(client: SlackRtmClient) extends Actor {

  def receive = gwState()

  def gwState(registered_full: Set[Subscription] = Set()): Receive = {
    case message: slack.models.Message if message.user != client.state.self.id =>
      println(s"GW $message")

      if (message.text == "SCRAM") {
        client.sendMessage(message.channel, "Shutting down")
        context.system.terminate()
      }

      for (Subscription(pattern, actor) <- registered_full)
        message.text match {
          case pattern(_*) => actor ! message
          case _ =>
            println(s"non matched $message")
        }

    // val mentionedIds = SlackUtil.extractMentionedIds(message.text)

    case PostToChannel(chan, msg) =>
      //TODO: proper rate limiting
      Thread sleep 1000
      client.sendMessage(chan, msg)


    case sub: Subscription =>
      context.become(gwState(registered_full + sub))
  }
}
