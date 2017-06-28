package bleh

import java.util

import ch.qos.logback.core.Context
import org.slf4j.{Logger, LoggerFactory, Marker}
import net.logstash.logback.marker.Markers._
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status


object oi extends App {


  val logger = new MyLoggerImpl

  logger.info("log message", "hello" -> 123)

}

trait MyLogger {

  def info(message: String, keysAndValues: (String, Any)*): Unit

}

class MyLoggerImpl extends MyLogger {

  val logger = LoggerFactory.getLogger("oi")

  override def info(message: String, keysAndValues: (String, Any)*): Unit = {
    logger.info(new MyMarker(keysAndValues), message)
  }

}

class MyMarker(val keysAndValues: Seq[(String, Any)]) extends Marker {
  override def hasChildren: Boolean = ???
  override def getName: String = ???
  override def remove(reference: Marker): Boolean = ???
  override def contains(other: Marker): Boolean = ???
  override def contains(name: String): Boolean = ???
  override def iterator(): util.Iterator[Marker] = ???
  override def add(reference: Marker): Unit = ???
  override def hasReferences: Boolean = ???
}

case class Annotation[T](instance: T, encoder: TypeEncoder[T])

sealed trait Value

case class StringValue(value: String) extends Value

trait TypeEncoder[T] {
  def encode(instance: T): Seq[(String, Value)]
}

class MyEncoder extends Encoder[ch.qos.logback.classic.spi.LoggingEvent] {

  var context: Context = _

  override def encode(event: ch.qos.logback.classic.spi.LoggingEvent): Array[Byte] = {
    println("encode " + event + " [" + event.getClass + "]")
    event.getMarker match {
      case mm: MyMarker =>
        println("my marker " + mm.keysAndValues)
      case _ =>
        println("unknown marker")
    }

    event.toString.getBytes()
  }

  override def headerBytes(): Array[Byte] = {
    println("headerBytes")
    ">>>".getBytes()
  }

  override def footerBytes(): Array[Byte] = {
    println("footerBytes")
    "<<<".getBytes()
  }

  override def stop(): Unit = {
    println("stop")
  }

  override def isStarted: Boolean = {
    println("isStarted")
    true
  }

  override def start(): Unit = {
    println("start")
  }

  override def addInfo(msg: String): Unit = {
    println("addInfo " + msg)
  }

  override def addInfo(msg: String, ex: Throwable): Unit = {
    println("addInfo " + msg)
  }

  override def addWarn(msg: String): Unit = {
    println("addWarn " + msg)
  }

  override def addWarn(msg: String, ex: Throwable): Unit = {
    println("addWarn " + msg)
  }

  override def addError(msg: String): Unit = {
    println("addError " + msg)
  }

  override def addError(msg: String, ex: Throwable): Unit = {
    println("addError " + msg)
  }

  override def addStatus(status: Status): Unit = {
    println("addStatus " + status)
  }

  override def getContext: Context = {
    println("getContext")
    context
  }

  override def setContext(context: Context): Unit = {
    println("setContext " + context)
    this.context = context
  }
}

/*

We implement against Logback
Can log to different formats (text, JSON, YAML, XML, ...)
Can accept maps, case classes, keys and values, ...
It is a Scala only library

 */