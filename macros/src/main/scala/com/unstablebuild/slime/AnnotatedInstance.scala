package com.unstablebuild.slime

case class AnnotatedInstance[T](instance: T, encoder: TypeEncoder[T]) {

  def encoded: Seq[(String, Value)] = encoder.encode(instance)

}
