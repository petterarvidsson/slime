package com.unstablebuild.slime

import java.io.{PrintWriter, StringWriter}

trait Encoders extends KeyedEncoders {

  implicit object throwableEncoder extends TypeEncoder[Throwable] {
    override def encode(instance: Throwable): Seq[(String, Value)] =
      Seq("exception" -> keyedThrowableEncoder.convert(instance))
  }

}

trait KeyedEncoders {

  class KeyedValueEncoder[V](val convert: V => Value) extends TypeEncoder[(String, V)] {
    override def encode(instance: (String, V)): Seq[(String, Value)] = Seq((instance._1, convert(instance._2)))
  }

  implicit object keyedStringEncoder extends KeyedValueEncoder[String](s => StringValue(s))
  implicit object keyedBooleanEncoder extends KeyedValueEncoder[Boolean](c => BooleanValue(c))
  implicit object keyedCharEncoder extends KeyedValueEncoder[Char](c => CharValue(c))
  implicit def keyedNumberEncoder[N: Numeric]: KeyedValueEncoder[N] = new KeyedValueEncoder[N](n => NumberValue(n))

  implicit object keyedSymbolEncoder extends KeyedValueEncoder[Symbol](s => StringValue(s.name))

  implicit def keyedSeqEncoder[T](implicit te: KeyedValueEncoder[T]): KeyedValueEncoder[Seq[T]] =
    new KeyedValueEncoder[Seq[T]](seq => SeqValue(seq.map(te.convert)))
  implicit def keyedSetEncoder[T](implicit te: KeyedValueEncoder[T]): KeyedValueEncoder[Set[T]] =
    new KeyedValueEncoder[Set[T]](seq => SeqValue(seq.map(te.convert).toSeq))
  implicit def keyedMapEncoder[V](implicit te: KeyedValueEncoder[V]): KeyedValueEncoder[Map[String, V]] =
    new KeyedValueEncoder[Map[String, V]](map => NestedValue(map.mapValues(te.convert).toSeq))
  implicit def symbolKeyedMapEncoder[V](implicit te: KeyedValueEncoder[V]): KeyedValueEncoder[Map[Symbol, V]] =
    new KeyedValueEncoder[Map[Symbol, V]](map => NestedValue(map.map { case (k, v) => k.name -> te.convert(v) }.toSeq))

  implicit object keyedThrowableEncoder
      extends KeyedValueEncoder[Throwable]({ e =>
        val stackTrace = new StringWriter()
        val writer = new PrintWriter(stackTrace)
        e.printStackTrace(writer)
        writer.flush()
        writer.close()
        NestedValue(
          Seq("message" -> StringValue(e.getMessage), "stack" -> StringValue(new String(stackTrace.toString)))
        )
      })

  implicit def symbolKeyedTypeEncoder[T](implicit te: TypeEncoder[(String, T)]): TypeEncoder[(Symbol, T)] =
    (instance: (Symbol, T)) => {
      val (key, value) = instance
      te.encode(key.name -> value)
    }

  implicit def keyedTypeEncoder[V](implicit te: TypeEncoder[V]): TypeEncoder[(String, V)] =
    (instance: (String, V)) => {
      val (outer, value) = instance
      te.encode(value).map { case (inner, encoded) => outer -> NestedValue(Seq(inner -> encoded)) }
    }

}
