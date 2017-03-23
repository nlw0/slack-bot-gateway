package slackbot

import java.io._
import java.time.ZonedDateTime

import akka.actor.Actor

import scala.concurrent.duration._
import scala.io.Source
import scala.xml.XML

class RSSReadingBot(channelId: String, sourceURL: String, pollingPeriod: FiniteDuration,
                    pattern: Option[scala.util.matching.Regex]) extends Actor {

  import context.dispatcher

  val slackOutput = context.actorSelection("/user/slack-gateway/output")

  context.system.scheduler.schedule(0.seconds, pollingPeriod, self, "tick")

  // def lastDate:ZonedDateTime = ZonedDateTime.now()
  def zeroDate: ZonedDateTime = ZonedDateTime.parse("0000-12-25T22:30+02:00")

  def dbFilename = s"/tmp/bot-${sourceURL.hashCode}.txt"

  def lastPersisted = scala.util.Try {
    ZonedDateTime.parse(Source.fromFile(dbFilename).getLines().mkString)
  }

  def receive = newsSeenControl(lastPersisted.toOption.getOrElse(zeroDate))

  def newsSeenControl(lastPublished: ZonedDateTime): Receive = {
    case "tick" =>
      println(s"checking out $sourceURL")

      val sd = XML.load(sourceURL)

      val defaultDate = findDateOptional(sd \\ "date").orElse(Some(zeroDate))

      def newNews = for {
        x <- (sd \\ "item").take(3)
        date <- findDateOptional(x \ "date").orElse(defaultDate)
        title = (x \ "title").text
        description = (x \ "description").text
        if date isAfter lastPublished
        processedText = (title + " " + description).toLowerCase //.split(" ").mkString
        if pattern.forall(_.findFirstIn(processedText).isDefined)
      } yield (date, x)

      val newsToPublish = newNews.sortWith({ case ((da, _), (db, _)) => da isBefore db }).take(3)

      for ((date, x) <- newsToPublish) {
        val msg = s"${(x \ "title").text} ${(x \ "link").text}"
        println(s"RSS publishing $msg")
        slackOutput ! PostToChannel(channelId, msg)
      }

      if (newsToPublish.nonEmpty) {
        val lastDate = newsToPublish.last._1
        persist(lastDate)
        context.become(newsSeenControl(lastDate))
      }
  }

  def persist(time: ZonedDateTime): Unit = {
    val file = new File(dbFilename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(time.toString + "\n")
    bw.close()
  }

  def findDateOptional(ns: scala.xml.NodeSeq) =
    ns.headOption.map({ n: scala.xml.Node => ZonedDateTime.parse(n.text) })
}
