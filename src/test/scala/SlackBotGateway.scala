import akka.actor.{ActorSystem, Props}
import slack.rtm.SlackRtmClient
import slackbot.{GatewayActor, RSSReadingBot, SumActor}


object SlackBotGateway extends App {
  implicit val system = ActorSystem("slack")

  val token = sys.env("SLACK_BOT_TOKEN")

  val client: SlackRtmClient = SlackRtmClient(token)

  val gwActor = system.actorOf(Props(classOf[GatewayActor], client), name = "slack-gateway")

  client.addEventListener(gwActor)

  gwActor ! client

  val sumActor = system.actorOf(Props[SumActor], name = "sum")
  val slashdotURL = "http://rss.slashdot.org/Slashdot/slashdotMain"
  val arxivURL = "http://export.arxiv.org/rss/cs.AI"
  val slashdotActor = system.actorOf(Props(classOf[RSSReadingBot], slashdotURL, None), name = "slashdot")
  val arxivActor = system.actorOf(Props(classOf[RSSReadingBot], arxivURL, Some("deep|autonomous driving".r)), name =
    "arxiv")
}
