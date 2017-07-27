package com.unstablebuild.slime

import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.{immutable, mutable}
import scala.util.control.NoStackTrace

class TypeEncodersTest extends FlatSpec with MustMatchers with TypeEncoders {

  it must "encode keyed and non-keyed exceptions" in {
    val exception = new Exception("BOOM!!!") with NoStackTrace
    val stack = exception.toString + "\n"
    encode(exception) must equal(
      Seq("exception" -> NestedValue(Seq("message" -> StringValue("BOOM!!!"), "stack" -> StringValue(stack))))
    )
    encode("error" -> exception) must equal(
      Seq("error" -> NestedValue(Seq("message" -> StringValue("BOOM!!!"), "stack" -> StringValue(stack))))
    )
  }

  it must "encode keyed primitive types" in {

    encode("hello" -> "world") must equal(Seq("hello" -> StringValue("world")))
    encode("hello" -> 'world) must equal(Seq("hello" -> StringValue("world")))

    encode("hello" -> 123) must equal(Seq("hello" -> NumberValue(123)))
    encode("hello" -> 123L) must equal(Seq("hello" -> NumberValue(123L)))
    encode("hello" -> 3.14f) must equal(Seq("hello" -> NumberValue(3.14f)))
    encode("hello" -> 3.14d) must equal(Seq("hello" -> NumberValue(3.14d)))
    encode("hello" -> BigInt(1)) must equal(Seq("hello" -> NumberValue(BigInt(1))))

    encode("hello" -> true) must equal(Seq("hello" -> BooleanValue(true)))
    encode("hello" -> false) must equal(Seq("hello" -> BooleanValue(false)))

    encode("hello" -> 'y') must equal(Seq("hello" -> StringValue("y")))
  }

  it must "encode types keyed by symbol or string" in {

    encode('hello -> "world") must equal(Seq("hello" -> StringValue("world")))
    encode('hello -> 123) must equal(Seq("hello" -> NumberValue(123)))
    encode('hello -> true) must equal(Seq("hello" -> BooleanValue(true)))
    encode('hello -> 'y') must equal(Seq("hello" -> StringValue("y")))
  }

  it must "encode collections" in {

    encode("hello" -> Array(1, 2, 3)) must equal(
      Seq("hello" -> SeqValue(Seq(NumberValue(1), NumberValue(2), NumberValue(3))))
    )
    encode("hello" -> Array('a -> ('b -> 2))) must equal(
      Seq("hello" -> NestedValue(Seq("a" -> NestedValue(Seq("b" -> NumberValue(2))))))
    )
    encode(Array('a -> 1, 'b -> 2)) must equal(Seq("a" -> NumberValue(1), "b" -> NumberValue(2)))

    encode("hello" -> Seq(1, 2, 3)) must equal(
      Seq("hello" -> SeqValue(Seq(NumberValue(1), NumberValue(2), NumberValue(3))))
    )
    encode("hello" -> List(1, 2, 3)) must equal(
      Seq("hello" -> SeqValue(Seq(NumberValue(1), NumberValue(2), NumberValue(3))))
    )
    encode("hello" -> mutable.ListBuffer(1, 2, 3)) must equal(
      Seq("hello" -> SeqValue(Seq(NumberValue(1), NumberValue(2), NumberValue(3))))
    )

    encode("hello" -> Set(1)) must equal(Seq("hello" -> SeqValue(Seq(NumberValue(1)))))
    encode("hello" -> immutable.HashSet(1)) must equal(Seq("hello" -> SeqValue(Seq(NumberValue(1)))))

    encode("hello" -> Set('a -> 1)) must equal(Seq("hello" -> NestedValue(Seq("a" -> NumberValue(1)))))
    encode("hello" -> Set('a -> ('b -> 2))) must equal(
      Seq("hello" -> NestedValue(Seq("a" -> NestedValue(Seq("b" -> NumberValue(2))))))
    )

    encode("hello" -> Seq(1, 2, 3).par) must equal(
      Seq("hello" -> SeqValue(Seq(NumberValue(1), NumberValue(2), NumberValue(3))))
    )

    encode(Map('a -> 1, 'b -> 2)) must equal(Seq("a" -> NumberValue(1), "b" -> NumberValue(2)))
    encode("hello" -> Map('a -> 1, 'b -> 2)) must equal(
      Seq("hello" -> NestedValue(Seq("a" -> NumberValue(1), "b" -> NumberValue(2))))
    )
    encode("hello" -> Map('a -> Map("a" -> 1), 'b -> Map("b" -> 2))) must equal(
      Seq(
        "hello" -> NestedValue(
          Seq("a" -> NestedValue(Seq("a" -> NumberValue(1))), "b" -> NestedValue(Seq("b" -> NumberValue(2))))
        )
      )
    )

    encode(Seq('a -> 1, 'b -> 2)) must equal(Seq("a" -> NumberValue(1), "b" -> NumberValue(2)))
    encode("hello" -> Seq('a -> 1, 'b -> 2)) must equal(
      Seq("hello" -> NestedValue(Seq("a" -> NumberValue(1), "b" -> NumberValue(2))))
    )
  }

  it must "encode options" in {
    encode("hello" -> Option.empty[String]) must equal(Seq("hello" -> SeqValue(Seq.empty)))
    encode("hello" -> Option("hi")) must equal(Seq("hello" -> SeqValue(Seq(StringValue("hi")))))
  }

  def encode[T](instance: T)(implicit te: TypeEncoder[T]): Seq[(String, Value)] =
    te.encode(instance)

}
