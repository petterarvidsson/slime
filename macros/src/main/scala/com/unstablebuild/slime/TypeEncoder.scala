package com.unstablebuild.slime

trait TypeEncoder[-T] {

  def encode(instance: T): Seq[(String, Value)]

}
