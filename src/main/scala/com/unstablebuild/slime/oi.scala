package com.unstablebuild.slime

import java.nio.charset.StandardCharsets

import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder

import scala.collection.JavaConverters._
import ch.qos.logback.core.status.Status
import ch.qos.logback.core.encoder.{Encoder => LogbackEncoder}
import ch.qos.logback.classic.spi.LoggingEvent
import com.unstablebuild.slime.format.Text

trait Format {

  def format(values: Seq[(String, Value)]): Array[Byte]

}





class Encoder extends LogbackEncoder[LoggingEvent] {

  val debug = false

  var context: Context = _

  var format: Format = new Text

  var fields: Seq[String] = Seq("level", "message")

  val fieldExtractors: Map[String, LoggingEvent => Value] =
    Map(
      "level" -> (e => StringValue(e.getLevel.toString)),
      "message" -> (e => StringValue(e.getMessage)),
      "threadName" -> (e => StringValue(e.getThreadName)),
      "loggerName" -> (e => StringValue(e.getLoggerName)),
      "mdc" -> (e => NestedValue(e.getMDCPropertyMap.asScala.mapValues(StringValue).toSeq)),
      "timestamp" -> (e => NumberValue(e.getTimeStamp))
    )

  override def encode(event: LoggingEvent): Array[Byte] = {
    if (debug) println("encode " + event + " [" + event.getClass + "]")

    // for compatibility with other users of logback, get event.getArgumentArray and pass to a default formatter

    val encodedData = event.getMarker match {
      case mm: AnnotationMarker =>
        if (debug) println("my marker " + mm.annotations)
        mm.encoded
      case _ =>
        if (debug) println("unknown marker")
        Seq.empty
    }

    val baseValues = fields.flatMap(f => fieldExtractors.get(f).map(extract => f -> extract(event)))

    format.format(baseValues ++ encodedData)
  }

  override def headerBytes(): Array[Byte] = {
    if (debug) println("headerBytes")
    Array.emptyByteArray
  }

  override def footerBytes(): Array[Byte] = {
    if (debug) println("footerBytes")
    Array.emptyByteArray
  }

  override def stop(): Unit = {
    if (debug) println("stop")
  }

  override def isStarted: Boolean = {
    if (debug) println("isStarted")
    true
  }

  override def start(): Unit = {
    if (debug) println("start")
  }

  override def addInfo(msg: String): Unit = {
    if (debug) println("addInfo " + msg)
  }

  override def addInfo(msg: String, ex: Throwable): Unit = {
    if (debug) println("addInfo " + msg)
  }

  override def addWarn(msg: String): Unit = {
    if (debug) println("addWarn " + msg)
  }

  override def addWarn(msg: String, ex: Throwable): Unit = {
    if (debug) println("addWarn " + msg)
  }

  override def addError(msg: String): Unit = {
    if (debug) println("addError " + msg)
  }

  override def addError(msg: String, ex: Throwable): Unit = {
    if (debug) println("addError " + msg)
  }

  override def addStatus(status: Status): Unit = {
    if (debug) println("addStatus " + status)
  }

  override def getContext: Context = {
    if (debug) println("getContext")
    context
  }

  override def setContext(context: Context): Unit = {
    if (debug) println("setContext " + context)
    this.context = context
  }

  def setFormat(format: Format): Unit = {
    if (debug) println(s"setFormat ${format.getClass}")
    this.format = format
  }

  def setFields(allFields: String): Unit = {
    if (debug) println("setFields " + allFields)
    this.fields = allFields.split(",").map(_.toLowerCase.trim)
  }

}

/*

We implement against Logback
Can log to different formats (text, JSON, YAML, XML, ...)
Can accept maps, case classes, keys and values, ...
It is a Scala only library

 */

@SlimeLogger
class MacroLogger extends Logger {}

object MacroLoggerTest extends App with Encoders {

  val logger = new MacroLogger

  logger.info("oi")
  logger.info("oi", "and" -> "tchau")

  locally {
    // could drop the first field and just use a type encoder that will transform a string into a message

    implicit object stringEncoder extends TypeEncoder[String] {
      override def encode(instance: String): Seq[(String, Value)] =
        Seq("message" -> StringValue(instance))
    }

    implicit object symbolEncoder extends TypeEncoder[Symbol] {
      override def encode(instance: Symbol): Seq[(String, Value)] =
        Seq("message" -> StringValue(instance.name))
    }

    logger.info("oi", "oi")
    logger.info("oi", 'oi_there)
  }

  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789.0, "a" -> true, "b" -> 'b')

  logger.info("log message", 'symbol -> 123)
  logger.info("log message", 'symbol -> 'to_symbol)

  logger.info("sequence", "numbers" -> Seq(1, 2, 3))
  logger.info("set", "chars" -> Set('a', 'b', 'c'))
  logger.info("map", "ages" -> Map("me" -> 10, "you" -> 12))
  logger.info("map", "ages" -> Map('me -> 10, 'you -> 12))

  logger.info("sequence", "numbers" -> Seq("vai" -> 123))
  logger.info("sequence", "numbers" -> Set("foi" -> 789))
  logger.info("sequence", "numbers" -> Map("is" -> Map("nested" -> Map("down" -> true, "up" -> false))))

  logger.info("exception", new Exception("ex1"))
  logger.info("exception pair", "first" -> new Exception("ex2"))

  logger.info("nested", "going" -> ("down" -> ("the" -> ("rabbit" -> "hole"))))

}
