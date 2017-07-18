package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime._

class Json extends Format {

  override def format(values: Seq[(String, Value)]): Array[Byte] = {
    (formatNested(values) + "\n").getBytes(StandardCharsets.UTF_8)
  }

  private def formatValue(value: Value): String = value match {
    case SeqValue(values) => values.map(formatValue).mkString("[", ",", "]")
    case StringValue(str) => "\"" + str.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t") + "\""
    case NumberValue(num) => num.toString
    case CharValue(c) => "\"" + c.toString + "\""
    case BooleanValue(b) => b.toString
    case NestedValue(values) => formatNested(values)
  }

  private def formatNested(values: Seq[(String, Value)]): String =
    values.map { case (k, v) => "\"" + k + "\":" + formatValue(v) }.mkString("{", ",", "}")

}
