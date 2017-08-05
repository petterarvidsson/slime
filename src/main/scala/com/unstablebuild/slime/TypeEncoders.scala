package com.unstablebuild.slime

import java.io.{PrintWriter, StringWriter}

import shapeless._
import shapeless.labelled.FieldType

import scala.collection.GenTraversable
import scala.language.higherKinds

trait TypeEncoders extends LowPriorityTypeEncoders {

  implicit val stringValuable: Valuable[String] = s => StringValue(s)
  implicit val symbolValuable: Valuable[Symbol] = s => StringValue(s.name)
  implicit val charValuable: Valuable[Char] = c => StringValue(c.toString)
  implicit def numberValuable[N: Numeric]: Valuable[N] = n => NumberValue(n)
  implicit val boolValuable: Valuable[Boolean] = b => BooleanValue(b)
  implicit val throwableValuable: Valuable[Throwable] = { e =>
    val stackTrace = new StringWriter()
    val writer = new PrintWriter(stackTrace)
    e.printStackTrace(writer)
    writer.flush()
    writer.close()
    NestedValue(Seq("message" -> StringValue(e.getMessage), "stack" -> StringValue(new String(stackTrace.toString))))
  }

  implicit val stringKey: Keyable[String] = (s: String) => s
  implicit val symbolKey: Keyable[Symbol] = (s: Symbol) => s.name

  implicit def traversableCollection[V, C[_] <: GenTraversable[_]]: Collection[V, C] =
    (traversable: C[V]) => traversable.seq.asInstanceOf[Traversable[V]]

  implicit def arrayCollection[V]: Collection[V, Array] =
    (array: Array[V]) => array.toTraversable

  implicit def optionCollection[V]: Collection[V, Option] =
    (opt: Option[V]) => opt.toTraversable

  implicit def keyValueEncoder[K: Keyable, V: Valuable]: TypeEncoder[(K, V)] =
    (instance: (K, V)) => Seq(implicitly[Keyable[K]].get(instance._1) -> implicitly[Valuable[V]].get(instance._2))

  implicit def traversableValuable[V, C[_]](implicit value: Valuable[V],
                                            collection: Collection[V, C]): Valuable[C[V]] =
    t => SeqValue(collection.traversable(t).map(v => value.get(v)).toSeq)

  implicit def traversablePairEncoder[K, V, C[_]](implicit te: TypeEncoder[(K, V)],
                                                  collection: Collection[(K, V), C]): TypeEncoder[C[(K, V)]] =
    t => collection.traversable(t).flatMap(te.encode).toSeq

}

object TypeEncoders extends TypeEncoders

// https://stackoverflow.com/a/1887678
trait LowPriorityTypeEncoders {

  implicit def keyOtherEncoder[K: Keyable, V: TypeEncoder]: TypeEncoder[(K, V)] = (instance: (K, V)) => {
    Seq(implicitly[Keyable[K]].get(instance._1) -> NestedValue(implicitly[TypeEncoder[V]].encode(instance._2)))
  }

  implicit def throwableEncoder(implicit throwableValuable: Valuable[Throwable]): TypeEncoder[Throwable] =
    instance => Seq("exception" -> throwableValuable.get(instance))

  implicit val hnilEncoder: TypeEncoder[HNil] =
    _ => Seq.empty

  implicit def hlistEncoder[K <: Symbol, H, T <: HList](implicit wit: Witness.Aux[K],
                                                        hEncoder: Lazy[TypeEncoder[(String, H)]],
                                                        tEncoder: TypeEncoder[T]): TypeEncoder[FieldType[K, H] :: T] =
    hlist => hEncoder.value.encode(wit.value.name -> hlist.head) ++ tEncoder.encode(hlist.tail)

  implicit def genericEncoder[T, R](implicit gen: LabelledGeneric.Aux[T, R], enc: TypeEncoder[R]): TypeEncoder[T] =
    obj => enc.encode(gen.to(obj))

}

trait Keyable[K] {
  def get(key: K): String
}

trait Valuable[-V] {
  def get(instance: V): Value
}

trait Collection[V, C[_]] {
  def traversable(collection: C[V]): Traversable[V]
}
