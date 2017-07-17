package com.unstablebuild.slime

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets

import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status

trait encoders {

  class KeyedValueEncoder[V](val convert: V => Value) extends TypeEncoder[(String, V)] {
    override def encode(instance: (String, V)): Seq[(String, Value)] = Seq((instance._1, convert(instance._2)))
  }

  implicit object keyedStringEncoder extends KeyedValueEncoder[String](s => StringValue(s))
  implicit object keyedBooleanEncoder extends KeyedValueEncoder[Boolean](c => BooleanValue(c))
  implicit object keyedCharEncoder extends KeyedValueEncoder[Char](c => CharValue(c))
  implicit def keyedNumberEncoder[N: Numeric]: KeyedValueEncoder[N] = new KeyedValueEncoder[N](n => NumberValue(n))

  implicit object keyedThrowableEncoder
      extends KeyedValueEncoder[Throwable]({ e =>
        val stackTrace = new StringWriter()
        val writer = new PrintWriter(stackTrace)
        e.printStackTrace(writer)
        writer.flush()
        writer.close()
        NestedValue(
          Seq("message" -> StringValue(e.getMessage), "stack" -> StringValue(new String(stackTrace.toString)))
        )
      })

  implicit def symbolKeyedTypeEncoder[T](implicit te: TypeEncoder[(String, T)]): TypeEncoder[(Symbol, T)] =
    (instance: (Symbol, T)) => {
      val (key, value) = instance
      te.encode(key.name -> value)
    }

  implicit def keyedTypeEncoder[V](implicit te: TypeEncoder[V]): TypeEncoder[(String, V)] =
    (instance: (String, V)) => {
      val (outer, value) = instance
      te.encode(value).map { case (inner, encoded) => outer -> NestedValue(Seq(inner -> encoded)) }
    }

  implicit object throwableEncoder extends TypeEncoder[Throwable] {
    override def encode(instance: Throwable): Seq[(String, Value)] =
      Seq("exception" -> keyedThrowableEncoder.convert(instance))
  }

}

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
    case NestedValue(values) => values.flatMap { case (k, v) => expand(prefix + "." + k, v) }
    case s: SingleValue => Seq(prefix -> s)
  }

}

class JsonFormat extends Format {

  override def format(message: String, values: Seq[(String, Value)]): Array[Byte] =
    (formatNested(("msg", StringValue(message)) +: values) + "\n").getBytes(StandardCharsets.UTF_8)

  private def formatValue(value: Value): String = value match {
    case StringValue(str) => "\"" + str.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t") + "\""
    case NumberValue(num) => num.toString
    case CharValue(c) => "\"" + c.toString + "\""
    case BooleanValue(b) => b.toString
    case NestedValue(values) => formatNested(values)
  }

  private def formatNested(values: Seq[(String, Value)]): String =
    values.map { case (k, v) => "\"" + k + "\":" + formatValue(v) }.mkString("{", ",", "}")

}

class MyEncoder extends Encoder[ch.qos.logback.classic.spi.LoggingEvent] {

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

  def setGol(string: String): Unit = {
    println(string)
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

object MacroLoggerTest extends App with encoders {

  val logger = new MacroLogger

  logger.info("oi")
  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789.0, "a" -> true, "b" -> 'b')

  logger.info("log message", 'symbol -> 123)

  logger.info("exception", new Exception("ex1"))
  logger.info("exception pair", "first" -> new Exception("ex2"))

  logger.info("nested", "going" -> ("down" -> ("the" -> ("rabbit" -> "hole"))))

}
