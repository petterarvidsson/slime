package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime._

class Json extends SimpleFormat {

  override def format(values: Seq[(String, Value)]): Array[Byte] =
    formatValue(NestedValue(values))(StringBuilder.newBuilder).append("\n").toString().getBytes(StandardCharsets.UTF_8)

  private def formatValue(value: Value)(implicit builder: StringBuilder): StringBuilder = value match {
    case NumberValue(num) =>
      builder.append(num.toString)
    case BooleanValue(b) =>
      builder.append(b.toString)
    case StringValue(str) =>
      builder.append('"')
      formatString(str)
      builder.append('"')
    case SeqValue(values) =>
      builder.append('[')
      if (values.nonEmpty) {
        formatValue(values.head)
        for (next <- values.tail) {
          builder.append(',')
          formatValue(next)
        }
      }
      builder.append(']')
    case NestedValue(values) =>
      @inline
      def appendPair(pair: (String, Value)): Unit = {
        builder.append('"')
        formatString(pair._1)
        builder.append("\":")
        formatValue(pair._2)
      }

      builder.append("{")
      if (values.nonEmpty) {
        appendPair(values.head)
        for (next <- values.tail) {
          builder.append(',')
          appendPair(next)
        }
      }
      builder.append("}")
  }

  private def formatString(str: String)(implicit builder: StringBuilder): Unit = str.foreach {
    case '\b' => builder.append("\\b")
    case '\f' => builder.append("\\f")
    case '\n' => builder.append("\\n")
    case '\r' => builder.append("\\r")
    case '\t' => builder.append("\\t")
    case '"' => builder.append("\\\"")
    case '\\' => builder.append("\\\\")
    case c if Character.isISOControl(c) => builder.append('\\').append("u%04x".format(c.toInt))
    case other => builder.append(other)
  }

}
