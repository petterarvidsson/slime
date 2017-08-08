package com.unstablebuild.slime

trait Format {

  def format(event: Seq[(String, Value)], data: Seq[(String, Value)]): Array[Byte]

}

trait SimpleFormat extends Format {

  override final def format(event: Seq[(String, Value)], data: Seq[(String, Value)]): Array[Byte] =
    format(event ++ data)

  def format(values: Seq[(String, Value)]): Array[Byte]

}
