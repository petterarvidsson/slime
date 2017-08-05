package com.unstablebuild.slime

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.LoggingEvent
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Random

class LoggerTest extends FlatSpec with MustMatchers {

  implicit val intTypeEncoder: TypeEncoder[Int] = (i: Int) => Seq(i.toString -> NumberValue(i))

  it must "provide methods to check the level set" in new loggerContext {

    override val loggerId: String = "com.unstablebuild.slime.LoggerTest.Level"

    logger.isTraceEnabled must be(false)
    logger.isDebugEnabled must be(false)
    logger.isInfoEnabled must be(true)
    logger.isWarnEnabled must be(true)
    logger.isErrorEnabled must be(true)
  }

  it must "log only if the level is enabled" in new loggerContext {

    override val loggerId: String = "com.unstablebuild.slime.LoggerTest.Level"

    logger.trace("!")
    logger.debug("!")
    logger.info("!")
    logger.warn("!")
    logger.error("!")

    levels must equal(Seq(Level.INFO, Level.WARN, Level.ERROR))
  }

  it must "provide trace logging methods" in new loggerContext {

    logger.trace("hi")
    logger.trace("hi", 1)
    logger.trace("hi", 1, 2)
    logger.trace("hi", 1, 2, 3)
    logger.trace("hi", 1, 2, 3, 4)
    logger.trace("hi", 1, 2, 3, 4, 5)

    all(messages) must equal("hi")
    all(levels) must equal(Level.TRACE)
    annotations must equal(expectedAnnotationsUpTo(5))
  }

  it must "provide debug logging methods" in new loggerContext {

    logger.debug("hallo")
    logger.debug("hallo", 1)
    logger.debug("hallo", 1, 2)
    logger.debug("hallo", 1, 2, 3)
    logger.debug("hallo", 1, 2, 3, 4)
    logger.debug("hallo", 1, 2, 3, 4, 5)

    all(messages) must equal("hallo")
    all(levels) must equal(Level.DEBUG)
    annotations must equal(expectedAnnotationsUpTo(5))
  }

  it must "provide info logging methods" in new loggerContext {

    logger.info("oi")
    logger.info("oi", 1)
    logger.info("oi", 1, 2)
    logger.info("oi", 1, 2, 3)
    logger.info("oi", 1, 2, 3, 4)
    logger.info("oi", 1, 2, 3, 4, 5)

    all(messages) must equal("oi")
    all(levels) must equal(Level.INFO)
    annotations must equal(expectedAnnotationsUpTo(5))
  }

  it must "provide warn logging methods" in new loggerContext {

    logger.warn("hey")
    logger.warn("hey", 1)
    logger.warn("hey", 1, 2)
    logger.warn("hey", 1, 2, 3)
    logger.warn("hey", 1, 2, 3, 4)
    logger.warn("hey", 1, 2, 3, 4, 5)

    all(messages) must equal("hey")
    all(levels) must equal(Level.WARN)
    annotations must equal(expectedAnnotationsUpTo(5))
  }

  it must "provide error logging methods" in new loggerContext {

    logger.error("ciao")
    logger.error("ciao", 1)
    logger.error("ciao", 1, 2)
    logger.error("ciao", 1, 2, 3)
    logger.error("ciao", 1, 2, 3, 4)
    logger.error("ciao", 1, 2, 3, 4, 5)

    all(messages) must equal("ciao")
    all(levels) must equal(Level.ERROR)
    annotations must equal(expectedAnnotationsUpTo(5))
  }

  trait loggerContext {

    val loggerId = s"${classOf[LoggerTest].getName}.${Random.alphanumeric.take(16).mkString}"

    lazy val logger = Logger(loggerId)

    def events: Seq[LoggingEvent] =
      MemoryAppender.events.filter(_.getLoggerName == loggerId)

    def annotations: Seq[Seq[AnnotatedInstance[_]]] =
      events.map(_.getMarker.asInstanceOf[AnnotationMarker].annotations)

    def messages: Seq[String] = events.map(_.getMessage)

    def levels: Seq[Level] = events.map(_.getLevel)

  }

  def expectedAnnotationsUpTo(count: Int): Seq[Seq[AnnotatedInstance[_]]] =
    (0 to count).map(i => (1 to i).map(j => AnnotatedInstance(j, intTypeEncoder)))

}
