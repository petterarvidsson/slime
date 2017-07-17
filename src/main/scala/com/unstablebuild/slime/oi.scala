package com.unstablebuild.slime

import java.nio.charset.StandardCharsets

import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status

trait Format {

  def format(message: String, values: Seq[(String, Value)]): Array[Byte]

}

class TextFormat extends Format {

  override def format(message: String, values: Seq[(String, Value)]): Array[Byte] = {
    values
      .flatMap((expand _).tupled)
      .map { case (k, v) => s"$k=${formatValue(v)}" }
      .mkString(s"$message\t\t", ",\t", "\n")
      .getBytes(StandardCharsets.UTF_8)
  }

  private def formatValue(value: SingleValue): String = SingleValue.unapply(value).mkString

  private def expand(prefix: String, value: Value): Seq[(String, SingleValue)] = value match {
    case SeqValue(values) => Seq(prefix -> StringValue(values.mkString("[", ",", "]")))
    case NestedValue(values) => values.flatMap { case (k, v) => expand(prefix + "." + k, v) }
    case s: SingleValue => Seq(prefix -> s)
  }

}

class JsonFormat extends Format {

  override def format(message: String, values: Seq[(String, Value)]): Array[Byte] =
    (formatNested(("msg", StringValue(message)) +: values) + "\n").getBytes(StandardCharsets.UTF_8)

  private def formatValue(value: Value): String = value match {
    case SeqValue(values) => values.map(formatValue).mkString("[", ",", "]")
    case StringValue(str) => "\"" + str.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t") + "\""
    case NumberValue(num) => num.toString
    case CharValue(c) => "\"" + c.toString + "\""
    case BooleanValue(b) => b.toString
    case NestedValue(values) => formatNested(values)
  }

  private def formatNested(values: Seq[(String, Value)]): String =
    values.map { case (k, v) => "\"" + k + "\":" + formatValue(v) }.mkString("{", ",", "}")

}

class SlimeEncoder extends Encoder[ch.qos.logback.classic.spi.LoggingEvent] {

  val debug = false

  var context: Context = _

  var format: Format = new TextFormat

  override def encode(event: ch.qos.logback.classic.spi.LoggingEvent): Array[Byte] = {
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

//    val dataAsString = encodedData.map { case (k, v) => s"$k=$v" }.mkString(", ")
//    s"-=$event [$dataAsString]=-\n".getBytes
    format.format(event.getMessage, encodedData)
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

  def setFormat(name: String): Unit = {
    if (debug) println(s"serializer is: $name")
    format = Class.forName(name).newInstance().asInstanceOf[Format]
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
  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789.0, "a" -> true, "b" -> 'b')

  logger.info("log message", 'symbol -> 123)
  logger.info("log message", 'symbol -> 'to_symbol)

  logger.info("sequence", "numbers" -> Seq(1, 2, 3))
  logger.info("set", "chars" -> Set('a', 'b', 'c'))
  logger.info("map", "ages" -> Map("me" -> 10, "you" -> 12))
  logger.info("map", "ages" -> Map('me -> 10, 'you -> 12))

  logger.info("exception", new Exception("ex1"))
  logger.info("exception pair", "first" -> new Exception("ex2"))

  logger.info("nested", "going" -> ("down" -> ("the" -> ("rabbit" -> "hole"))))

}
