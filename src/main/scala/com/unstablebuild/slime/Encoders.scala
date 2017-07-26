package com.unstablebuild.slime

import java.io.{PrintWriter, StringWriter}

import scala.collection.GenTraversable

trait Encoders extends LowPriorityEncoders {

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

  implicit def traversableCollection[A, C[_] <: GenTraversable[_]]: Collection[A, C] =
    (traversable: C[A]) => traversable.seq.asInstanceOf[Traversable[A]]

  implicit def arrayCollection[A]: Collection[A, Array] =
    (array: Array[A]) => array.toTraversable

  implicit def optionCollection[A]: Collection[A, Option] =
    (opt: Option[A]) => opt.toTraversable

  implicit def keyValueEncoder[K: Keyable, V: Valuable]: TypeEncoder[(K, V)] = (instance: (K, V)) => {
    Seq(implicitly[Keyable[K]].get(instance._1) -> implicitly[Valuable[V]].get(instance._2))
  }

  implicit def traversableValuable[V, C[_]](implicit value: Valuable[V],
                                            collection: Collection[V, C]): Valuable[C[V]] =
    t => SeqValue(collection.traversable(t).map(v => value.get(v)).toSeq)

  implicit def traversablePairEncoder[K, V, C[_]](implicit te: TypeEncoder[(K, V)],
                                                  collection: Collection[(K, V), C]): TypeEncoder[C[(K, V)]] =
    t => collection.traversable(t).flatMap(te.encode).toSeq

  implicit object encodableEncoder extends TypeEncoder[Encodable] {
    override def encode(instance: Encodable): Seq[(String, Value)] = instance.encoded
  }

}

// https://stackoverflow.com/a/1887678
trait LowPriorityEncoders {

  implicit def keyOtherEncoder[K: Keyable, V: TypeEncoder]: TypeEncoder[(K, V)] = (instance: (K, V)) => {
    Seq(implicitly[Keyable[K]].get(instance._1) -> NestedValue(implicitly[TypeEncoder[V]].encode(instance._2)))
  }

  implicit def throwableEncoder(implicit throwableValuable: Valuable[Throwable]): TypeEncoder[Throwable] =
    instance => Seq("exception" -> throwableValuable.get(instance))

}

trait Keyable[T] {
  def get(key: T): String
}

trait Valuable[-T] {
  def get(instance: T): Value
}

trait Collection[A, C[_]] {
  def traversable(collection: C[A]): Traversable[A]
}
