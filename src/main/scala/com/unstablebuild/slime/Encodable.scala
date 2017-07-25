package com.unstablebuild.slime

trait Encodable {

  def encoded: Seq[(String, Value)]

}
