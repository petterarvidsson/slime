package com.unstablebuild.slime

trait Format {

  def format(values: Seq[(String, Value)]): Array[Byte]

}
