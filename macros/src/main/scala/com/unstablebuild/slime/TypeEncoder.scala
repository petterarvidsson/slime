package com.unstablebuild.slime

trait TypeEncoder[-T] {

  def encode(instance: T): Seq[(String, Value)]

}

object TypeEncoder {

  def apply[T](implicit te: TypeEncoder[T]): TypeEncoder[T] = te

}
