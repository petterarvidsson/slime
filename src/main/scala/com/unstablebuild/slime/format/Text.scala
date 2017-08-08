package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime._

class Text extends SimpleFormat {

  var separator = "  "

  override def format(values: Seq[(String, Value)]): Array[Byte] = {
    values
      .flatMap((expand _).tupled)
      .map((formatPair _).tupled)
      .mkString("", separator, "\n")
      .getBytes(StandardCharsets.UTF_8)
  }

  protected def formatPair(key: String, value: SingleValue): String =
    s"$key=${formatValue(value)}"

  protected def formatValue(value: SingleValue): String = SingleValue.unapply(value).mkString

  protected def expand(prefix: String, value: Value): Seq[(String, SingleValue)] = value match {
    case seq: SeqValue => Seq(prefix -> StringValue(formatSeq(prefix, seq)))
    case NestedValue(values) => values.flatMap { case (k, v) => expand(s"$prefix.$k", v) }
    case s: SingleValue => Seq(prefix -> s)
  }

  protected def formatSeq(prefix: String, seq: SeqValue): String = {
    val strings = seq.values.map {
      case SingleValue(v) => v.toString
      case other => expand("", other).map { case (k, v) => s"$k=${formatValue(v)}" }.mkString("{", ",", "}")
    }
    strings.mkString("[", ",", "]")
  }

  def setSeparator(separator: String): Unit =
    this.separator = separator.replaceAll("\\\\t", "\t").replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r")

}
