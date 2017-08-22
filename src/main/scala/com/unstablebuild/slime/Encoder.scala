package com.unstablebuild.slime

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.encoder.EncoderBase
import com.unstablebuild.slime.field.{DefaultFieldExtractor, FieldExtractor}
import com.unstablebuild.slime.format.Text

class Encoder extends EncoderBase[LoggingEvent] {

  private val debug = false

  var format: Format = new Text

  var fields: Seq[String] = Seq("level", "message")

  var fieldExtractor: FieldExtractor = new DefaultFieldExtractor

  override def encode(event: LoggingEvent): Array[Byte] = {
    doDebug(s"encode $event")

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
    format.format(extractFields(event), values)
  }

  def extractFields(event: LoggingEvent): Seq[(String, Value)] = {
    fields.flatMap(f => fieldExtractor.extract(f, event).map(f -> _))
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

  def setFieldExtractor(fieldExtractor: FieldExtractor): Unit = {
    this.fieldExtractor = fieldExtractor
  }

  @inline
  private def doDebug(msg: => String): Unit = {
    if (debug) addInfo(msg)
  }

}
