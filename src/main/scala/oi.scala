package bleh

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets

import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status

trait encoders {

  class KeyedValueEncoder[V](val convert: V => Value) extends TypeEncoder[(String, V)] {
    override def encode(instance: (String, V)): Seq[(String, Value)] = Seq((instance._1, convert(instance._2)))
  }

  implicit object keyedStringEncoder extends KeyedValueEncoder[String](s => StringValue(s))
  implicit object keyedNumberEncoder extends KeyedValueEncoder[Int](i => NumberValue(i))
  implicit object keyedThrowableEncoder extends KeyedValueEncoder[Throwable]({ e =>
    val stackTrace = new StringWriter()
    val writer = new PrintWriter(stackTrace)
    e.printStackTrace(writer)
    writer.flush()
    writer.close()
    NestedValue(Seq("message" -> StringValue(e.getMessage), "stack" -> StringValue(new String(stackTrace.toString))))
  })

  implicit def keyedTypeEncoder[V](implicit te: TypeEncoder[V]): TypeEncoder[(String, V)] = (instance: (String, V)) => {
    val (outer, value) = instance
    te.encode(value).map { case (inner, encoded) => outer -> NestedValue(Seq(inner -> encoded)) }
  }

  implicit object throwableEncoder extends TypeEncoder[Throwable] {
    override def encode(instance: Throwable): Seq[(String, Value)] =
      Seq("exception" -> keyedThrowableEncoder.convert(instance))
  }

}

trait Format {

  def format(message: String, values: Seq[(String, Value)]): Array[Byte]

}

class TextFormat extends Format {

  override def format(message: String, values: Seq[(String, Value)]): Array[Byte] = {
    values
      .flatMap((expand _).tupled)
      .map { case (k, v) => s"$k=${formatValue(v)}" }
      .mkString(s"$message\t\t", ",\t", "\n")
      .getBytes(StandardCharsets.UTF_8)
  }

  private def formatValue(value: SingleValue): String = value match {
    case StringValue(str) => str
    case NumberValue(num) => num.toString
  }

  private def expand(prefix: String, value: Value): Seq[(String, SingleValue)] = value match {
    case NestedValue(values) => values.flatMap { case (k, v) => expand(prefix + "." + k, v) }
    case s: SingleValue => Seq(prefix -> s)
  }

}

class JsonFormat extends Format {

  override def format(message: String, values: Seq[(String, Value)]): Array[Byte] =
    (formatNested(("msg", StringValue(message)) +: values) + "\n").getBytes(StandardCharsets.UTF_8)

  private def formatValue(value: Value): String = value match {
    case StringValue(str) => "\"" + str.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t") + "\""
    case NumberValue(num) => num.toString
    case NestedValue(values) => formatNested(values)
  }

  private def formatNested(values: Seq[(String, Value)]): String =
    values.map { case (k, v) => "\"" + k + "\":" + formatValue(v) }.mkString("{", ",", "}")

}

class MyEncoder extends Encoder[ch.qos.logback.classic.spi.LoggingEvent] {

  val debug = false

  var context: Context = _

  var format: Format = new TextFormat

  override def encode(event: ch.qos.logback.classic.spi.LoggingEvent): Array[Byte] = {
    if (debug) println("encode " + event + " [" + event.getClass + "]")

    // for compatibility with other users of logback, get event.getArgumentArray and pass to a default formatter

    val encodedData = event.getMarker match {
      case mm: AnnotationMarker =>
        if (debug) println("my marker " + mm.annotations)
        mm.encoded
      case _ =>
        if (debug) println("unknown marker")
        Seq.empty
    }

//    val dataAsString = encodedData.map { case (k, v) => s"$k=$v" }.mkString(", ")
//    s"-=$event [$dataAsString]=-\n".getBytes
    format.format(event.getMessage, encodedData)
  }

  override def headerBytes(): Array[Byte] = {
    if (debug) println("headerBytes")
    Array.emptyByteArray
  }

  override def footerBytes(): Array[Byte] = {
    if (debug) println("footerBytes")
    Array.emptyByteArray
  }

  override def stop(): Unit = {
    if (debug) println("stop")
  }

  override def isStarted: Boolean = {
    if (debug) println("isStarted")
    true
  }

  override def start(): Unit = {
    if (debug) println("start")
  }

  override def addInfo(msg: String): Unit = {
    if (debug) println("addInfo " + msg)
  }

  override def addInfo(msg: String, ex: Throwable): Unit = {
    if (debug) println("addInfo " + msg)
  }

  override def addWarn(msg: String): Unit = {
    if (debug) println("addWarn " + msg)
  }

  override def addWarn(msg: String, ex: Throwable): Unit = {
    if (debug) println("addWarn " + msg)
  }

  override def addError(msg: String): Unit = {
    if (debug) println("addError " + msg)
  }

  override def addError(msg: String, ex: Throwable): Unit = {
    if (debug) println("addError " + msg)
  }

  override def addStatus(status: Status): Unit = {
    if (debug) println("addStatus " + status)
  }

  override def getContext: Context = {
    if (debug) println("getContext")
    context
  }

  override def setContext(context: Context): Unit = {
    if (debug) println("setContext " + context)
    this.context = context
  }

  def setFormat(name: String): Unit = {
    if (debug) println(s"serializer is: $name")
    format = Class.forName(name).newInstance().asInstanceOf[Format]
  }

  def setGol(string: String): Unit = {
    println(string)
  }

}

/*

We implement against Logback
Can log to different formats (text, JSON, YAML, XML, ...)
Can accept maps, case classes, keys and values, ...
It is a Scala only library

 */



// perhaps we don't need this
// could also try http://docs.scala-lang.org/overviews/quasiquotes/usecases#offline-code-generation
trait Logger {

  def isErrorEnabled: Boolean
  def isInfoEnabled: Boolean
  def isWarnEnabled: Boolean
  def isTraceEnabled: Boolean
  def isDebugEnabled: Boolean
  def error  (message: => String ) : Unit
  def error [T1] (message: => String , t1: => T1) (implicit te1: TypeEncoder[T1]): Unit
  def error [T1, T2] (message: => String , t1: => T1, t2: => T2) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2]): Unit
  def error [T1, T2, T3] (message: => String , t1: => T1, t2: => T2, t3: => T3) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3]): Unit
  def error [T1, T2, T3, T4] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4]): Unit
  def error [T1, T2, T3, T4, T5] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5]): Unit
  def error [T1, T2, T3, T4, T5, T6] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21]): Unit
  def error [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21, t22: => T22) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21], te22: TypeEncoder[T22]): Unit
  def info  (message: => String ) : Unit
  def info [T1] (message: => String , t1: => T1) (implicit te1: TypeEncoder[T1]): Unit
  def info [T1, T2] (message: => String , t1: => T1, t2: => T2) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2]): Unit
  def info [T1, T2, T3] (message: => String , t1: => T1, t2: => T2, t3: => T3) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3]): Unit
  def info [T1, T2, T3, T4] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4]): Unit
  def info [T1, T2, T3, T4, T5] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5]): Unit
  def info [T1, T2, T3, T4, T5, T6] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21]): Unit
  def info [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21, t22: => T22) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21], te22: TypeEncoder[T22]): Unit
  def warn  (message: => String ) : Unit
  def warn [T1] (message: => String , t1: => T1) (implicit te1: TypeEncoder[T1]): Unit
  def warn [T1, T2] (message: => String , t1: => T1, t2: => T2) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2]): Unit
  def warn [T1, T2, T3] (message: => String , t1: => T1, t2: => T2, t3: => T3) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3]): Unit
  def warn [T1, T2, T3, T4] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4]): Unit
  def warn [T1, T2, T3, T4, T5] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5]): Unit
  def warn [T1, T2, T3, T4, T5, T6] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21]): Unit
  def warn [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21, t22: => T22) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21], te22: TypeEncoder[T22]): Unit
  def trace  (message: => String ) : Unit
  def trace [T1] (message: => String , t1: => T1) (implicit te1: TypeEncoder[T1]): Unit
  def trace [T1, T2] (message: => String , t1: => T1, t2: => T2) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2]): Unit
  def trace [T1, T2, T3] (message: => String , t1: => T1, t2: => T2, t3: => T3) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3]): Unit
  def trace [T1, T2, T3, T4] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4]): Unit
  def trace [T1, T2, T3, T4, T5] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5]): Unit
  def trace [T1, T2, T3, T4, T5, T6] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21]): Unit
  def trace [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21, t22: => T22) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21], te22: TypeEncoder[T22]): Unit
  def debug  (message: => String ) : Unit
  def debug [T1] (message: => String , t1: => T1) (implicit te1: TypeEncoder[T1]): Unit
  def debug [T1, T2] (message: => String , t1: => T1, t2: => T2) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2]): Unit
  def debug [T1, T2, T3] (message: => String , t1: => T1, t2: => T2, t3: => T3) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3]): Unit
  def debug [T1, T2, T3, T4] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4]): Unit
  def debug [T1, T2, T3, T4, T5] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5]): Unit
  def debug [T1, T2, T3, T4, T5, T6] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21]): Unit
  def debug [T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22] (message: => String , t1: => T1, t2: => T2, t3: => T3, t4: => T4, t5: => T5, t6: => T6, t7: => T7, t8: => T8, t9: => T9, t10: => T10, t11: => T11, t12: => T12, t13: => T13, t14: => T14, t15: => T15, t16: => T16, t17: => T17, t18: => T18, t19: => T19, t20: => T20, t21: => T21, t22: => T22) (implicit te1: TypeEncoder[T1], te2: TypeEncoder[T2], te3: TypeEncoder[T3], te4: TypeEncoder[T4], te5: TypeEncoder[T5], te6: TypeEncoder[T6], te7: TypeEncoder[T7], te8: TypeEncoder[T8], te9: TypeEncoder[T9], te10: TypeEncoder[T10], te11: TypeEncoder[T11], te12: TypeEncoder[T12], te13: TypeEncoder[T13], te14: TypeEncoder[T14], te15: TypeEncoder[T15], te16: TypeEncoder[T16], te17: TypeEncoder[T17], te18: TypeEncoder[T18], te19: TypeEncoder[T19], te20: TypeEncoder[T20], te21: TypeEncoder[T21], te22: TypeEncoder[T22]): Unit


}

@slimeLogger
class MacroLogger extends Logger {

}

object MacroLoggerTest extends App with encoders {

  val logger = new MacroLogger

  logger.info("oi")
  logger.info("log message", "hello" -> 123, "world" -> 456, "!" -> 789)

  logger.info("exception", new Exception("ex1"))
  logger.info("exception pair", "first" -> new Exception("ex2"))

  logger.info("nested", "going" -> ("down" -> ("the" -> ("rabbit" -> "hole"))))

}
