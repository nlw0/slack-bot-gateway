import akka.actor.{ActorSystem, Props}
import slack.rtm.SlackRtmClient
import slackbot._

import scala.concurrent.duration._
import scala.io.Source


object SlackBotGateway extends App {
  implicit val system = ActorSystem("slack")

  val token = sys.env("SLACK_BOT_TOKEN")

  val client: SlackRtmClient = SlackRtmClient(token)

  val gwActor = system.actorOf(Props(classOf[GatewayActor], client), name = "slack-gateway")

  client.addEventListener(gwActor)

  val sumActor = system.actorOf(Props[SumActor], name = "sum")

  val pluginsActor = system.actorOf(Props(classOf[PluginsActor], 10.seconds, client.state.self.id), name = "plugins")

  val rssConfigRow = raw"^(\S+)\t(\d+)\t(\S+)(\t.+)?$$".r

  val rssConfigFile = "rss.conf"
  val rssActors = Source.fromFile(rssConfigFile).getLines foreach {
    case rssConfigRow(channel, period_, url, pat) =>
      println(s"RSS -> $channel, $period_, $url, $pat")
      val channelId = client.state.getChannelIdForName(channel)
      val period = period_.toInt.minutes
      val regex = if (pat == null) None else Some(pat.tail.r)
      for (cid <- channelId)
        system.actorOf(Props(classOf[RSSReadingBot], cid, url, period, regex), name = "rss" + url.slice(7, 14))

    case _ =>
      println("RSS config not matched")
  }

  //  val slashdotURL = "http://rss.slashdot.org/Slashdot/slashdotMain"
  //  val slashdotActor = system.actorOf(Props(classOf[RSSReadingBot], slashdotURL, 2.minutes, None), name = "slashdot")
  //
  //  val arxivURL = "http://export.arxiv.org/rss/cs.AI"
  //  val arxivActor = system.actorOf(
  //    Props(classOf[RSSReadingBot], arxivURL, 30.minutes, Some("deep|autonomous driving".r)), name = "arxiv")
}
