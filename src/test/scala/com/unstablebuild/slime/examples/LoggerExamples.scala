package com.unstablebuild.slime.examples

import com.unstablebuild.slime.{Encodable, Logger, NumberValue, StringValue, TypeEncoder, Value}

object LoggerExamples extends App {

  val logger = Logger("hi")

  logger.info("oi")
  logger.info("oi", "and" -> "tchau")

  locally {
    // could drop the first field and just use a type encoder that will transform a string into a message

    implicit object stringEncoder extends TypeEncoder[String] {
      override def encode(instance: String): Seq[(String, Value)] =
        Seq("message" -> StringValue(instance))
    }

    implicit object symbolEncoder extends TypeEncoder[Symbol] {
      override def encode(instance: Symbol): Seq[(String, Value)] =
        Seq("message" -> StringValue(instance.name))
    }

    logger.info("oi", "oi")
    logger.info("oi", 'oi_there)
  }

  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789.0, "a" -> true, "b" -> 'b')

  logger.info("log message", 'symbol -> 123)
  logger.info("log message", 'symbol -> 'to_symbol)

  logger.info("sequence", "numbers" -> Seq(1, 2, 3))
  logger.info("set", "chars" -> Set('a', 'b', 'c'))
  logger.info("map", "ages" -> Map("me" -> 10, "you" -> 12))
  logger.info("map", "ages" -> Map('me -> 10, 'you -> 12))

  logger.info("sequence", "numbers" -> Seq("vai" -> 123))
  logger.info("sequence", "numbers" -> Set("foi" -> 789))
  logger.info("sequence", "numbers" -> Map("is" -> Map("nested" -> Map("down" -> true, "up" -> false))))

  logger.info("exception", new Exception("ex1"))
  logger.info("exception pair", "first" -> new Exception("ex2"))

  logger.info("nested", "going" -> ("down" -> ("the" -> ("rabbit" -> "hole"))))

  case class SomeType(int: Int, str: String) extends Encodable {
    override def encoded: Seq[(String, Value)] = Seq("int" -> NumberValue(int), "str" -> StringValue(str))
  }

  logger.info("encodable", SomeType(1, "a"))
  logger.info("encodable", "keyed" -> SomeType(2, "b"))

}
