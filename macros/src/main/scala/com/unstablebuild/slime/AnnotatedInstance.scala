package com.unstablebuild.slime

case class AnnotatedInstance[T](instance: T, encoder: TypeEncoder[T]) {

  def encoded: Seq[(String, Value)] = encoder.encode(instance)

}

object AnnotatedInstance {

  def from[T](instance: T)(implicit encoder: TypeEncoder[T]): AnnotatedInstance[T] =
    apply(instance, encoder)

}
