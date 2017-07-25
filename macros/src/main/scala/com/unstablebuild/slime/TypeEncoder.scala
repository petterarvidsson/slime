package com.unstablebuild.slime

trait TypeEncoder[-T] {

  def encode(instance: T): Seq[(String, Value)]

}

class KeyedValueEncoder[V](val convert: V => Value) extends TypeEncoder[(String, V)] {

  override def encode(instance: (String, V)): Seq[(String, Value)] = Seq((instance._1, convert(instance._2)))

}
