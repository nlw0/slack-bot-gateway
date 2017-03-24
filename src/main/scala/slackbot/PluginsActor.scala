package slackbot

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import akka.actor.Actor
import slack.SlackUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.sys.process._
import scala.util.matching.Regex

class PluginsActor(pollingPeriod: FiniteDuration, botId: String) extends Actor {

  import context.dispatcher

  val slackOutput = context.actorSelection("/user/slack-gateway/output")

  val slackGw = context.actorSelection("/user/slack-gateway")
  slackGw ! Subscription(".*", self)

  val pluginsDirectory = Paths.get("plugins")
  val pluginsConfigFile = Paths.get("plugins.conf").toFile

  context.system.scheduler.schedule(0.seconds, pollingPeriod, self, UpdatePlugins)

  def receive = pluginsConfig()

  def pluginsConfig(lastConfigUpdate: Long = 0, routingTable: Seq[BotTrigger] = Nil): Receive = {
    case UpdatePlugins =>
      if (pluginsConfigFile.lastModified > lastConfigUpdate) {
        println("Reading new plugins configuration")
        context.become(pluginsConfig(pluginsConfigFile.lastModified, readNewConfig))
      }

    case m: slack.models.Message =>
      for (BotTrigger(context, pat, script) <- routingTable)
        if (pat.findFirstIn(m.text).isDefined
            && (context != BotMentioned || SlackUtil.extractMentionedIds(m.text).contains(botId))
            && (context != PrivateMessage || m.channel.head == 'D')
        ) {
          val is = new ByteArrayInputStream(m.text.getBytes("UTF-8"))

          val processOutput = (script #< is).lineStream.iterator

          val output = processOutput.mkString("\n")

          slackOutput ! PostToChannel(m.channel, s"<@${m.user}> $output")
        }
  }

  val configRow = raw"(^[map])\t([^\t]+)\t([a-zA-Z0-9.]+)$$".r

  def readNewConfig: List[BotTrigger] = {
    Source.fromFile(pluginsConfigFile).getLines collect {
      case configRow(msgType, reg, prog) =>
        val scriptName = pluginsDirectory.resolve(prog).toFile.toString
        msgType match {
          case "m" => BotTrigger(BotMentioned, reg.r, scriptName)
          case "a" => BotTrigger(AllMessages, reg.r, scriptName)
          case "p" => BotTrigger(PrivateMessage, reg.r, scriptName)
        }
    } toList
  }

  case object UpdatePlugins

}

sealed trait TriggerContext

case object BotMentioned extends TriggerContext

case object AllMessages extends TriggerContext

case object PrivateMessage extends TriggerContext

case class BotTrigger(context: TriggerContext, pattern: Regex, script: String)

