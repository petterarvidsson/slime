package com.unstablebuild.slime

trait Logging {

  protected def logger: Logger

}

trait LazyLogging extends Logging {

  protected lazy val logger: Logger = Logger(getClass.getName)

}

trait StrictLogging extends Logging {

  protected val logger: Logger = Logger(getClass.getName)

}
