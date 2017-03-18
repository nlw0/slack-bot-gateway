import akka.actor.{ActorSystem, Props}
import slack.rtm.SlackRtmClient
import slackbot.{GatewayActor, SlashdotActor, SumActor}


object SlackBotGateway extends App {
  implicit val system = ActorSystem("slack")

  val token = sys.env("SLACK_BOT_TOKEN")

  val client: SlackRtmClient = SlackRtmClient(token)

  val gwActor = system.actorOf(Props(classOf[GatewayActor], client), name = "slack-gateway")

  client.addEventListener(gwActor)

  gwActor ! client

  val greetActor = system.actorOf(Props[SumActor], name = "sum")
  val slashdotActor = system.actorOf(Props[SlashdotActor], name = "slashdot")
}
