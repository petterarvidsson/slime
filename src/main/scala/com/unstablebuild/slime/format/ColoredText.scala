package com.unstablebuild.slime.format

import com.unstablebuild.slime._

class ColoredText extends Text {

  private val boldMagentaStart = new String(Array[Byte](27, 91, 51, 53, 109))
  private val boldMagentaEnd = new String(Array[Byte](27, 91, 51, 57, 109))
  private val boldStart = new String(Array[Byte](61, 27, 91, 49, 109))
  private val boldEnd = new String(Array[Byte](27, 91, 50, 50, 109))

  override protected def formatPair(key: String, value: SingleValue): String =
    s"$boldMagentaStart$key$boldMagentaEnd$boldStart${formatValue(value)}$boldEnd"

}
