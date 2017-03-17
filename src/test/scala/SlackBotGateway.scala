import slack.rtm.SlackRtmClient
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import slack.SlackUtil

class GreetingActor extends Actor {
  val gwActor = context.actorSelection("/user/slack-gateway")
  gwActor ! "register_all"

  def receive = {
    case m => println(s"Recebi!! $m")
  }
}

class GatewayActor(client: SlackRtmClient) extends Actor {

  def receive = gwState()

  def gwState(registered_full: Set[ActorRef]=Set()): Receive = {
    case message: slack.models.Message =>
      for (actor <- registered_full) actor ! message

      val mentionedIds = SlackUtil.extractMentionedIds(message.text)

      if (mentionedIds.contains(client.state.self.id)) {
        client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
      }

    case "register_all" =>
      context.become(gwState(registered_full + sender))
  }
}

object SlackBotGateway extends App {
  implicit val system = ActorSystem("slack")

  val token = sys.env("SLACK_BOT_TOKEN")

  val client: SlackRtmClient = SlackRtmClient(token)

  val gwActor = system.actorOf(Props(classOf[GatewayActor], client), name = "slack-gateway")

  client.addEventListener(gwActor)

  gwActor ! client

  val greetActor = system.actorOf(Props[GreetingActor], name="greet")

}
