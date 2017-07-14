package com.unstablebuild.slime

sealed trait Value

sealed trait SingleValue extends Value

case class StringValue(value: String) extends SingleValue
case class NumberValue(value: Number) extends SingleValue

case class NestedValue(values: Seq[(String, Value)]) extends Value
