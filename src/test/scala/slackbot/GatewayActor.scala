package slackbot

import akka.actor.{Actor, Props}
import slack.rtm.SlackRtmClient
import scala.concurrent.duration._

class GatewayOutputActor(client: SlackRtmClient, minimumWaitPeriod: FiniteDuration = 1.second) extends Actor {

  import context.dispatcher

  def receive = flowControlContext()

  def flowControlContext(lastSent: Long = 0L, delayedMessages: Int = 0): Receive = {
    case PostToChannel(chan, msg) =>
      val now = System.currentTimeMillis()
      if (now > lastSent + minimumWaitPeriod.toMillis) {
        client.sendMessage(chan, msg)
        context.become(flowControlContext(now, delayedMessages = 0 min (delayedMessages - 1)))
      } else {
        val newDelayedMessages = delayedMessages + 1
        context.system.scheduler.scheduleOnce(newDelayedMessages * minimumWaitPeriod, self, PostToChannel(chan, msg))
        context.become(flowControlContext(now, newDelayedMessages))
      }
  }
}

class GatewayActor(client: SlackRtmClient) extends Actor {

  val outputActor = context.actorOf(Props(classOf[GatewayOutputActor], client, 1.second), name = "output")

  def receive = gwState()

  def gwState(registered_full: Set[Subscription] = Set()): Receive = {
    case message: slack.models.Message if message.user != client.state.self.id =>
      println(s"GW $message")

      if (message.text == "SCRAM") {
        client.sendMessage(message.channel, "Shutting down")
        context.system.terminate()
      }

      for (Subscription(pattern, actor) <- registered_full) {
        println(Subscription(pattern, actor))

        message.text match {
          case pattern(_*) => actor ! message
          case _ =>
            println(s"non matched $message")
        }
      }
    // val mentionedIds = SlackUtil.extractMentionedIds(message.text)

    case sub: Subscription =>
      context.become(gwState(registered_full + sub))

    case se: slack.models.SlackEvent =>
      println(se)

    case m: PostToChannel =>
      outputActor ! m
  }
}
