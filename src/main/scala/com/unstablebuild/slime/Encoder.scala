package com.unstablebuild.slime

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.encoder.EncoderBase
import com.unstablebuild.slime.format.Text

import scala.collection.JavaConverters._

class Encoder extends EncoderBase[LoggingEvent] {

  private val debug = false

  var format: Format = new Text

  var fields: Seq[String] = Seq("level", "message")

  override def encode(event: LoggingEvent): Array[Byte] = {
    doDebug(s"encode $event [${event.getClass}]")

    val values = event.getMarker match {
      case mm: AnnotationMarker =>
        doDebug(s"marker ${mm.annotations}")
        mm.encoded
      case _ =>
        doDebug("unknown marker")
        Seq.empty
    }

    format(event, values)
  }

  def format(event: LoggingEvent, values: Seq[(String, Value)]): Array[Byte] = {
    format.format(extractFields(event) ++ values)
  }

  def extractFields(event: LoggingEvent): Seq[(String, Value)] = {
    fields.flatMap(f => Encoder.fieldExtractors.get(f).map(extract => f -> extract(event)))
  }

  override def headerBytes(): Array[Byte] = {
    Array.emptyByteArray
  }

  override def footerBytes(): Array[Byte] = {
    Array.emptyByteArray
  }

  def setFormat(format: Format): Unit = {
    doDebug(s"setFormat ${format.getClass}")
    this.format = format
  }

  def setFields(allFields: String): Unit = {
    doDebug(s"setFields $allFields")
    this.fields = allFields.split(",").map(_.toLowerCase.trim)
  }

  @inline
  private def doDebug(msg: => String): Unit = {
    if (debug) addInfo(msg)
  }

}

object Encoder {

  val fieldExtractors: Map[String, LoggingEvent => Value] =
    Map(
      "level" -> (e => StringValue(e.getLevel.toString)),
      "message" -> (e => StringValue(e.getFormattedMessage)),
      "thread" -> (e => StringValue(e.getThreadName)),
      "logger" -> (e => StringValue(e.getLoggerName)),
      "mdc" -> (e => NestedValue(e.getMDCPropertyMap.asScala.mapValues(StringValue).toSeq)),
      "timestamp" -> (e => NumberValue(e.getTimeStamp))
    )

}
