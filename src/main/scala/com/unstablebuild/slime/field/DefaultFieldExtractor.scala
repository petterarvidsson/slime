package com.unstablebuild.slime.field

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import ch.qos.logback.classic.spi.LoggingEvent
import com.unstablebuild.slime.{NestedValue, NumberValue, StringValue, Value}

import scala.collection.JavaConverters._

class DefaultFieldExtractor extends FieldExtractor {

  override def extract(field: String, event: LoggingEvent): Option[Value] =
    DefaultFieldExtractor.fieldExtractors.get(field).map(_.apply(event))

}

object DefaultFieldExtractor {

  private val dateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneOffset.UTC)

  val fieldExtractors: Map[String, LoggingEvent => Value] =
    Map(
      "level" -> (e => StringValue(e.getLevel.toString)),
      "message" -> (e => StringValue(e.getFormattedMessage)),
      "thread" -> (e => StringValue(e.getThreadName)),
      "logger" -> (e => StringValue(e.getLoggerName)),
      "mdc" -> (e => NestedValue(e.getMDCPropertyMap.asScala.mapValues(StringValue).toSeq)),
      "timestamp" -> (e => NumberValue(e.getTimeStamp)),
      "ts" -> (e => StringValue(dateFormatter.format(Instant.ofEpochMilli(e.getTimeStamp)))),
      "caller" -> (e => StringValue(caller(e.getCallerData)))
    )

  // The first entry comes from the logger library, being the 2nd element the actual caller
  private def caller(stack: Array[StackTraceElement]): String = stack match {
    case Array(_, c, _ *) => s"(${c.getFileName}:${c.getLineNumber})"
    case _ => "unknown"
  }

}
