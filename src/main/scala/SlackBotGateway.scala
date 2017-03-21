import akka.actor.{ActorSystem, Props}
import slack.rtm.SlackRtmClient
import slackbot.{GatewayActor, PluginsActor, RSSReadingBot, SumActor}

import scala.concurrent.duration._


object SlackBotGateway extends App {
  implicit val system = ActorSystem("slack")

  val token = sys.env("SLACK_BOT_TOKEN")

  val client: SlackRtmClient = SlackRtmClient(token)

  val gwActor = system.actorOf(Props(classOf[GatewayActor], client), name = "slack-gateway")

  client.addEventListener(gwActor)

  val sumActor = system.actorOf(Props[SumActor], name = "sum")

  val pluginsActor = system.actorOf(Props(classOf[PluginsActor], 10.seconds), name = "plugins")

  val slashdotURL = "http://rss.slashdot.org/Slashdot/slashdotMain"
  val slashdotActor = system.actorOf(Props(classOf[RSSReadingBot], slashdotURL, 2.minutes, None), name = "slashdot")

  val arxivURL = "http://export.arxiv.org/rss/cs.AI"
  val arxivActor = system.actorOf(
    Props(classOf[RSSReadingBot], arxivURL, 30.minutes, Some("deep|autonomous driving".r)), name = "arxiv")



}
