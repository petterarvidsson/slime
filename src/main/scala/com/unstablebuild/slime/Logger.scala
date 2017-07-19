package com.unstablebuild.slime

import com.unstablebuild.slime.impl.MacroLogger

import scala.reflect.ClassTag

@SlimeLogger
trait Logger

object Logger {

  def apply[T]()(implicit ct: ClassTag[T]): Logger = apply(ct.runtimeClass)
  def apply[T](clazz: Class[T]): Logger = apply(clazz.getName)
  def apply(name: String): Logger = new MacroLogger(name)

}
