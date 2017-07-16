package com.unstablebuild.slime

sealed trait Value

sealed trait SingleValue extends Value

case class StringValue(value: String) extends SingleValue
case class NumberValue[N: Numeric](value: N) extends SingleValue
case class CharValue(value: Char) extends SingleValue
case class BooleanValue(value: Boolean) extends SingleValue

case class NestedValue(values: Seq[(String, Value)]) extends Value
