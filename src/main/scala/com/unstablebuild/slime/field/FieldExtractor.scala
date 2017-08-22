package com.unstablebuild.slime.field

import ch.qos.logback.classic.spi.LoggingEvent
import com.unstablebuild.slime.Value

trait FieldExtractor {

  def extract(field: String, event: LoggingEvent): Option[Value]

}
