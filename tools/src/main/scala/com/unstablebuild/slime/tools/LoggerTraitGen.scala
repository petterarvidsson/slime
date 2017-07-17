package com.unstablebuild.slime.tools

import com.unstablebuild.slime.LoggerMacros

import scala.language.experimental.macros

object LoggerTraitGen {

  def codeLines: Array[String] = macro LoggerMacros.interface

  def main(args: Array[String]): Unit = {
    codeLines.foreach(println)
  }

}
