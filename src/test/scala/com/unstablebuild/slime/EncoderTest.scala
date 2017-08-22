package com.unstablebuild.slime

import ch.qos.logback.classic.spi.LoggingEvent
import org.scalatest.{FlatSpec, MustMatchers}
import ch.qos.logback.classic.Level
import org.slf4j.{MDC, Marker}

import scala.collection.mutable

class EncoderTest extends FlatSpec with MustMatchers { self =>

  it must "return the output of the format" in new encoderContext {
    encoder.encode(event()) must equal(format.format(Seq.empty))
  }

  it must "encode annotated values" in new encoderContext with TypeEncoders {

    val annotations = Seq(AnnotatedInstance.from("one" -> 1), AnnotatedInstance.from("two" -> 2))
    encoder.setFields("level")
    encoder.encode(event(marker = AnnotationMarker(annotations)))

    format.receivedValues must equal(
      Seq("level" -> StringValue("TRACE"), "one" -> NumberValue(1), "two" -> NumberValue(2))
    )
  }

  it must "add level and message" in new encoderContext {

    encoder.setFields("level,message")
    encoder.encode(event(msg = "hi!", level = Level.WARN))

    format.receivedValues must equal(Seq("level" -> StringValue("WARN"), "message" -> StringValue("hi!")))
  }

  it must "add mdc" in new encoderContext {

    encoder.setFields("mdc")
    MDC.put("hello", "world")
    encoder.encode(event())

    format.receivedValues must equal(Seq("mdc" -> NestedValue(Seq("hello" -> StringValue("world")))))
  }

  it must "add timestamp" in new encoderContext {
    encoder.setFields("timestamp,ts")
    encoder.encode(event(ts = 12345L))

    format.receivedValues must equal(
      Seq("timestamp" -> NumberValue(12345L), "ts" -> StringValue("1970-01-01T00:00:12.345Z"))
    )
  }

  it must "add thread and logger" in new encoderContext {
    encoder.setFields("thread, logger")
    encoder.encode(event())

    format.receivedValues must equal(
      Seq("thread" -> StringValue(Thread.currentThread().getName), "logger" -> StringValue(self.getClass.getName))
    )
  }

  trait encoderContext {

    val encoder = new Encoder
    val format = new RecodingFormat
    encoder.setFormat(format)

  }

  def event(msg: String = "", level: Level = Level.TRACE, ts: Long = 0, marker: Marker = null): LoggingEvent = {
    val event = new LoggingEvent()
    event.setLevel(level)
    event.setMessage(msg)
    event.setTimeStamp(ts)
    event.setThreadName(Thread.currentThread().getName)
    event.setLoggerName(getClass.getName)
    event.setMarker(marker)
    event
  }

  class RecodingFormat extends SimpleFormat {
    private val allValues = mutable.ListBuffer.empty[Seq[(String, Value)]]

    override def format(values: Seq[(String, Value)]): Array[Byte] = {
      allValues += values
      "hi".getBytes()
    }

    def receivedValues: Seq[(String, Value)] = allValues.flatten
  }

}
