package com.unstablebuild.slime

import java.util

import org.slf4j.Marker

import scala.annotation.StaticAnnotation
import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox



sealed trait Value

sealed trait SingleValue extends Value

case class StringValue(value: String) extends SingleValue
case class NumberValue(value: Number) extends SingleValue

case class NestedValue(values: Seq[(String, Value)]) extends Value


trait TypeEncoder[-T] {

  def encode(instance: T): Seq[(String, Value)]

}

case class AnnotatedInstance[T](instance: T, encoder: TypeEncoder[T]) {

  def encoded: Seq[(String, Value)] = encoder.encode(instance)

}


class AnnotationMarker(val annotations: Seq[AnnotatedInstance[_]]) extends Marker {

  def encoded: Seq[(String, Value)] = annotations.flatMap(_.encoded)

  override def hasChildren: Boolean = false
  override def getName: String = "annotation-marker"
  override def remove(reference: Marker): Boolean = false
  override def contains(other: Marker): Boolean = false
  override def contains(name: String): Boolean = false
  override def iterator(): util.Iterator[Marker] = Iterator.empty.asJava
  override def add(reference: Marker): Unit = ()
  override def hasReferences: Boolean = false

}

object LoggerMacros {

  val params: Seq[Int] = 0 to 22
  val levels: Set[Level] = org.slf4j.event.Level.values().map(l => Level(l.toString.toLowerCase())).toSet

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val generator = new LoggerGenerator(c)
    import generator.c.universe._

    val result = annottees.map(_.tree).headOption match {
      case Some(q"class $name extends ..$parents { ..$body }") =>

        val baseLogger = generator.val_baseLogger
        val baseMethods = (for (level <- levels) yield generator.def_isEnabled(level)).toList
        val loggingMethods = for (level <- levels; size <- params) yield generator.def_log(level, size)

        q"""
          class $name extends ..$parents {
            $baseLogger
            ..$body
            ..$baseMethods
            ..$loggingMethods
          }
        """
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
      q"""private val logger = org.slf4j.LoggerFactory.getLogger("oi")"""
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

      val types = defaultOrMkString(paramsCount)("[", ", ", "]", n => s"T$n")
      val params = defaultOrMkString(paramsCount)(", ", ", ", "", n => s"t$n: => T$n")
      val implicits = defaultOrMkString(paramsCount)("(implicit ", ", ", ")", n => s"te$n: $typeEncoderClass[T$n]")

      s"def ${level.lowerCase} $types (message: => String $params) $implicits: Unit"
    }

    def def_log(level: Level, paramsCount: Int): Tree = {
      val annotationClass = classOf[AnnotatedInstance[_]].getCanonicalName
      val annotationMarkerClass = classOf[AnnotationMarker].getCanonicalName

      val signature = def_log_signature(level, paramsCount)
      val annotations = defaultOrMkString(paramsCount, default = "Seq()")("Seq(", ", ", ")", n => s"$annotationClass(t$n, te$n)")

      val method = s"""
        $signature = {
          if (this.logger.${level.isEnabledName}) {
            this.logger.${level.lowerCase}(new $annotationMarkerClass($annotations), message)
          }
        }
      """

      c.parse(method)
    }

    private def defaultOrMkString(count: Int, default: String = "")(start: String, sep: String, end: String, each: Int => String): String = {
      if (count == 0) default else (1 to count).map(each).mkString(start, sep, end)
    }

  }

}

//private in the package
private[slime] class slimeLogger extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LoggerMacros.impl
}
