package bleh


import java.util

import org.slf4j.Marker

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import java.util
import scala.collection.JavaConverters._
import scala.reflect.macros.blackbox



sealed trait Value

case class StringValue(value: String) extends Value
case class NumberValue(value: Number) extends Value


trait TypeEncoder[T] {

  def encode(instance: T): Seq[(String, Value)]

}

case class Annotation[T](instance: T, encoder: TypeEncoder[T]) {

  def encoded: Seq[(String, Value)] = encoder.encode(instance)

}


class AnnotationMarker(val annotations: Seq[Annotation[_]]) extends Marker {

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

object helloMacro {

  case class Level(name: String) {

    def lowerCase: String = name.toLowerCase()

    def camelCase: String = name.substring(0, 1).toUpperCase + name.substring(1).toLowerCase

    def isEnabledName: String = s"is${camelCase}Enabled"


  }

  val params = 0 to 22
  val levels = org.slf4j.event.Level.values().map(l => Level(l.toString.toLowerCase())).toSet

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe.{Annotation => _, _}
    val result = {

      annottees.map(_.tree).toList match {
        case q"class $name extends ..$parents { ..$body }" :: Nil =>

          val typeEncoderClass = classOf[TypeEncoder[_]].getCanonicalName
          val annotationClass = classOf[Annotation[_]].getCanonicalName
          val annotationMarkerClass = classOf[AnnotationMarker].getCanonicalName

          val baseMethods = for (level <- levels) yield {
            val signature = s"def ${level.isEnabledName}: Boolean"
            val method = s"$signature = this.logger.${level.isEnabledName}"

//            println(method)

            c.parse(method)
          }

          val loggingMethods = for (level <- levels; size <- params) yield {

            val types = if (size == 0) "" else (1 to size).map(n => s"T$n").mkString("[", ", ", "]")
            val params = if (size == 0) "" else (1 to size).map(n => s"t$n: => T$n").mkString(", ", ", ", "")
            val implicits = if (size == 0) "" else (1 to size).map(n => s"te$n: $typeEncoderClass[T$n]").mkString("(implicit ", ", ", ")")
            val annotations = if (size == 0) "Seq()" else (1 to size).map(n => s"$annotationClass(t$n, te$n)").mkString("Seq(", ", ", ")")

            val signature = s"def ${level.lowerCase} $types (message: => String $params) $implicits: Unit"

            val method = s"""
              $signature = {
                if (this.logger.${level.isEnabledName}) {
                  this.logger.${level.lowerCase}(new $annotationMarkerClass($annotations), message)
                }
              }
            """

//            println(method)

            c.parse(method)
          }

          q"""
            class $name extends ..$parents {

              private val logger = org.slf4j.LoggerFactory.getLogger("oi")

              ..$body

              ..$baseMethods

              ..$loggingMethods

            }
          """
      }
    }
    c.Expr[Any](result)
  }
}

//private in the package
class slimeLogger extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro helloMacro.impl
}
