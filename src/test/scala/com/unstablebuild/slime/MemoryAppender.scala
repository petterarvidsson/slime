package com.unstablebuild.slime

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase

import scala.collection.mutable

class MemoryAppender extends UnsynchronizedAppenderBase[LoggingEvent] {

  override def append(event: LoggingEvent): Unit =
    MemoryAppender.appendedEvents.append(event)

}

object MemoryAppender {

  private[MemoryAppender] val appendedEvents = mutable.ListBuffer.empty[LoggingEvent]

  def events: Seq[LoggingEvent] = appendedEvents

}
