# *slime*

*slime* is a structured logging library for Scala. It is based on logback and provides a custom type-based logger interface plus encoders.

## Usage

Configuration is made through the same mechanisms made available by logback. For instance, you can use `logback.xml` to enable *slime*:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="com.unstablebuild.slime.Encoder" />
      <format class="com.unstablebuild.slime.format.Json" />
    </encoder>
  </appender>

  <root level="all">
    <appender-ref ref="console" />
  </root>

</configuration>
```

... then, given a piece of code like the one below:

```scala
case class User(username: String, emails: Set[String])

class Service extends LazyLogging {

  def register(user: User): Unit = {
    val id = //..
    logger.info("A new user was registered", "id" -> id, "user" -> user)
    id
  }

}
```

... would generate the following output:

```json
{"level":"INFO","message":"A new user was registered","id":123,"user":{"username":"slimedoe","emails":["slime.doe@example.com"]}}
```
