package com.unstablebuild.slime

sealed trait Value

sealed trait SingleValue extends Value

object SingleValue {

  def unapply(value: SingleValue): Option[Any] = value match {
    case StringValue(s) => Some(s)
    case NumberValue(n) => Some(n)
    case CharValue(c) => Some(c)
    case BooleanValue(b) => Some(b)
  }

}

case class StringValue(value: String) extends SingleValue
case class NumberValue[N: Numeric](value: N) extends SingleValue
case class CharValue(value: Char) extends SingleValue
case class BooleanValue(value: Boolean) extends SingleValue

case class SeqValue(values: Seq[Value]) extends Value
case class NestedValue(values: Seq[(String, Value)]) extends Value

object AnyValue {

  def unapply(value: Value): Option[Any] = value match {
    case SingleValue(any) => Some(any)
    case NestedValue(nested) => Some(nested)
  }

}
