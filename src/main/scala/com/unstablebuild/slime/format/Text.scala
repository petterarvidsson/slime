package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime.{Format, NestedValue, SeqValue, SingleValue, StringValue, Value}

class Text extends Format {

  override def format(values: Seq[(String, Value)]): Array[Byte] = {
    values
      .flatMap((expand _).tupled)
      .map { case (k, v) => s"$k=${formatValue(v)}" }
      .mkString("", ",\t", "\n")
      .getBytes(StandardCharsets.UTF_8)
  }

  private def formatValue(value: SingleValue): String = SingleValue.unapply(value).mkString

  private def expand(prefix: String, value: Value): Seq[(String, SingleValue)] = value match {
    case SeqValue(values) => Seq(prefix -> StringValue(values.mkString("[", ",", "]")))
    case NestedValue(values) => values.flatMap { case (k, v) => expand(prefix + "." + k, v) }
    case s: SingleValue => Seq(prefix -> s)
  }

}
