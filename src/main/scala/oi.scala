package bleh

import java.io.{PrintWriter, StringWriter}
import java.util

import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status
import scala.collection.JavaConverters._
import org.slf4j.{LoggerFactory, Marker}

trait encoders {

  class PairEncoder[V](convert: V => Value) extends TypeEncoder[(String, V)] {
    override def encode(instance: (String, V)): Seq[(String, Value)] = Seq((instance._1, convert(instance._2)))
  }

  implicit object keyedIntEncoder extends PairEncoder[Int](i => NumberValue(i))

  implicit object exceptionEncoder extends TypeEncoder[Exception] {
    override def encode(instance: Exception): Seq[(String, Value)] = {
      val stackTrace = new StringWriter()
      val writer = new PrintWriter(stackTrace)
      instance.printStackTrace(writer)
      writer.flush()
      writer.close()
      Seq("exceptionMessage" -> StringValue(instance.getMessage), "stackTrace" -> StringValue(new String(stackTrace.toString)))
    }
  }

}

object oi extends App with encoders {

  val logger = new MyLoggerImpl

  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789)
  logger.info("log message", new Exception("fuuuuuuuuu"))

}

trait MyLogger {

  def info(message: => String): Unit

  //scala-logging is using a macro to generate the logger https://github.com/typesafehub/scala-logging/blob/master/src/main/scala/com/typesafe/scalalogging/LoggerMacro.scala
  def info[T1: TypeEncoder](message: => String, d1: => T1): Unit

  def info[T1: TypeEncoder, T2: TypeEncoder](message: => String, d1: => T1, d2: => T2): Unit

  def info[T1: TypeEncoder, T2: TypeEncoder, T3: TypeEncoder](message: => String, d1: => T1, d2: => T2, d3: => T3): Unit

}

class MyLoggerImpl extends MyLogger {

  private val logger = LoggerFactory.getLogger("oi")

  override def info(message: => String): Unit = {
    if (logger.isInfoEnabled) {
      logger.info(message)
    }
  }

  override def info[T1: TypeEncoder](message: => String, d1: => T1): Unit = {
    if (logger.isInfoEnabled) {
      doInfo(message, Seq(Annotation(d1, implicitly[TypeEncoder[T1]])))
    }
  }

  override def info[T1: TypeEncoder, T2: TypeEncoder](message: => String, d1: => T1, d2: => T2): Unit = {
    if (logger.isInfoEnabled) {
      doInfo(message, Seq(Annotation(d1, implicitly[TypeEncoder[T1]]), Annotation(d2, implicitly[TypeEncoder[T2]])))
    }
  }

  override def info[T1: TypeEncoder, T2: TypeEncoder, T3: TypeEncoder](message: => String, d1: => T1, d2: => T2, d3: => T3): Unit = {
    if (logger.isInfoEnabled) {
      doInfo(message, Seq(Annotation(d1, implicitly[TypeEncoder[T1]]), Annotation(d2, implicitly[TypeEncoder[T2]]),
        Annotation(d3, implicitly[TypeEncoder[T3]])))
    }
  }

  private def doInfo(message: => String, annotations: => Seq[Annotation[_]]): Unit = {
    logger.info(new AnnotationMarker(annotations), message)
  }

}

class AnnotationMarker(val annotations: Seq[Annotation[_]]) extends Marker {

  def encoded: Seq[(String, Value)] = annotations.flatMap(_.encoded)

  override def hasChildren: Boolean = false
  override def getName: String = "annotation-marker"
  override def remove(reference: Marker): Boolean = false
  override def contains(other: Marker): Boolean = false
  override def contains(name: String): Boolean = false
  override def iterator(): util.Iterator[Marker] = Iterator.empty.asJava
  override def add(reference: Marker): Unit = ()
  override def hasReferences: Boolean = false
}

case class Annotation[T](instance: T, encoder: TypeEncoder[T]) {

  def encoded: Seq[(String, Value)] = encoder.encode(instance)

}

sealed trait Value

case class StringValue(value: String) extends Value
case class NumberValue(value: Number) extends Value

trait TypeEncoder[T] {

  def encode(instance: T): Seq[(String, Value)]

}

class MyEncoder extends Encoder[ch.qos.logback.classic.spi.LoggingEvent] {

  val debug = false

  var context: Context = _

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

    val dataAsString = encodedData.map { case (k, v) => s"$k=$v" }.mkString(", ")
    s"-=$event [$dataAsString]=-\n".getBytes
  }

  override def headerBytes(): Array[Byte] = {
    if (debug) println("headerBytes")
    ">>>\n".getBytes()
  }

  override def footerBytes(): Array[Byte] = {
    if (debug) println("footerBytes")
    "\n<<<\n".getBytes()
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
}

/*

We implement against Logback
Can log to different formats (text, JSON, YAML, XML, ...)
Can accept maps, case classes, keys and values, ...
It is a Scala only library

 */
