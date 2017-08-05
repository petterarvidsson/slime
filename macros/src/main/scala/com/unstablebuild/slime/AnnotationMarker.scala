package com.unstablebuild.slime

import java.util

import org.slf4j.Marker

import scala.collection.JavaConverters._

case class AnnotationMarker(annotations: Seq[AnnotatedInstance[_]]) extends Marker {

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
