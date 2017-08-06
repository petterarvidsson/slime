package com.unstablebuild.slime

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object LoggerMacros {

  val params: Seq[Int] = 0 to 22
  val levels: Seq[Level] = org.slf4j.event.Level.values().map(l => Level(l.toString.toLowerCase()))

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val generator = new LoggerGenerator(c)
    import generator.c.universe._

    val result = annottees.map(_.tree).headOption match {
      case Some(q"class $name extends ..$parents { ..$body }") =>
        val baseLogger = generator.val_baseLogger
        val baseMethods = for (level <- levels) yield generator.def_isEnabled(level)
        val loggingMethods = for (level <- levels; size <- params) yield generator.def_log(level, size)

        q"""
          class $name(val name: String) extends ..$parents {
            $baseLogger
            ..$body
            ..$baseMethods
            ..$loggingMethods
          }
        """
      case Some(q"abstract trait $name") =>
        val companion = annottees.map(_.tree).collectFirst {
          case obj @ q"object $_ extends $_ { ..$_ }" =>
            obj
        }

        val baseSignatures = for (level <- levels) yield generator.def_isEnabled_signature(level)
        val loggingSignatures = for (level <- levels; size <- params) yield generator.def_log_signature(level, size)

        val code = s"""
          trait $name {
            ${baseSignatures.mkString("", ";\n", ";\n")}
            ${loggingSignatures.mkString("", ";\n", ";\n")}
          }

          ${companion.map(t => showCode(t.asInstanceOf[Tree])).mkString}
        """.stripMargin

        c.parse(code)
    }

    c.Expr[Any](result.asInstanceOf[c.Tree])
  }

  case class Level(name: String) {

    def lowerCase: String = name.toLowerCase()

    def camelCase: String = name.substring(0, 1).toUpperCase + name.substring(1).toLowerCase

    def isEnabledName: String = s"is${camelCase}Enabled"

  }

  class LoggerGenerator(val c: blackbox.Context) {

    import c.universe._

    def val_baseLogger: Tree = {
      q"""private val logger = org.slf4j.LoggerFactory.getLogger(name)"""
    }

    def def_isEnabled_signature(level: Level): String = {
      s"def ${level.isEnabledName}: Boolean"
    }

    def def_isEnabled(level: Level): Tree = {
      val signature = def_isEnabled_signature(level)
      val method = s"$signature = this.logger.${level.isEnabledName}"

      c.parse(method)
    }

    def def_log_signature(level: Level, paramsCount: Int): String = {
      val typeEncoderClass = classOf[TypeEncoder[_]].getCanonicalName

      val types = generateForParams(paramsCount)("[", ", ", "]", n => s"T$n")
      val params = generateForParams(paramsCount)(", ", ", ", "", n => s"t$n: => T$n")
      val implicits = generateForParams(paramsCount)("(implicit ", ", ", ")", n => s"te$n: $typeEncoderClass[T$n]")

      s"def ${level.lowerCase} $types (message: => String $params) $implicits: Unit"
    }

    def def_log(level: Level, paramsCount: Int): Tree = {
      val annotationClass = classOf[AnnotatedInstance[_]].getCanonicalName
      val annotationMarkerClass = classOf[AnnotationMarker].getCanonicalName

      val signature = def_log_signature(level, paramsCount)
      val annotations =
        generateForParams(paramsCount, onZero = "Seq()")("Seq(", ", ", ")", n => s"$annotationClass(t$n, te$n)")

      val method = s"""
        $signature = {
          if (this.logger.${level.isEnabledName}) {
            this.logger.${level.lowerCase}(new $annotationMarkerClass($annotations), message)
          }
        }
      """

      c.parse(method)
    }

    private def generateForParams(
      count: Int,
      onZero: String = ""
    )(start: String, sep: String, end: String, each: Int => String): String = {
      if (count == 0) onZero else (1 to count).map(each).mkString(start, sep, end)
    }

  }

}

/**
  * This is kept private on the package so it cannot be (easily) used externally.
  */
private[slime] class SlimeLogger extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LoggerMacros.impl
}
