package slackbot

import akka.actor.Actor

import scala.io.Source
import java.io.{ByteArrayInputStream, File}
import java.nio.file.Paths

import scala.sys.process._
import scala.concurrent.duration._
import scala.util.matching.Regex

class PluginsActor(pollingPeriod: FiniteDuration) extends Actor {

  import context.dispatcher

  val slackOutput = context.actorSelection("/user/slack-gateway/output")

  val slackGw = context.actorSelection("/user/slack-gateway")
  slackGw ! Subscription(".*".r, self)

  val pluginsDirectory = Paths.get("plugins")
  val pluginsConfigFile = Paths.get("plugins.conf").toFile

  context.system.scheduler.schedule(0.seconds, pollingPeriod, self, UpdatePlugins)

  def receive = pluginsConfig()

  def pluginsConfig(lastConfigUpdate: Long = 0, routingTable: Seq[(Regex, String)] = Nil): Receive = {
    case UpdatePlugins =>
      if (pluginsConfigFile.lastModified > lastConfigUpdate) {
        println("Reading new plugins configuration")
        context.become(pluginsConfig(pluginsConfigFile.lastModified, readNewConfig))
      }

    case m: slack.models.Message =>
      for ((pat, prog) <- routingTable)
        if (pat.findFirstIn(m.text).isDefined) {
          val is = new ByteArrayInputStream(m.text.getBytes("UTF-8"))
          val output = (prog #< is).lineStream.mkString("\n")
          slackOutput ! PostToChannel(m.channel, s"<@${m.user}> $output")
        }
  }

  val configRow = raw"(^[^\t]+)\t([a-zA-Z0-9.]+)$$".r

  def readNewConfig: List[(Regex, String)] = {
    Source.fromFile(pluginsConfigFile).getLines collect {
      case configRow(reg, prog) => reg.r -> pluginsDirectory.resolve(prog).toFile
    } map {
      case (pattern, file) => (pattern, file.toString)
    } toList
  }

  case object UpdatePlugins

}


