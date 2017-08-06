package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.{Json => PlayJson}

class JsonTest extends FlatSpec with MustMatchers {

  val format = new Json

  it must "produce valid json for all fields" in {
    formatted(
      Seq(
        "s" -> StringValue("hello!"),
        "ni" -> NumberValue(7),
        "nf" -> NumberValue(3.4567),
        "b" -> BooleanValue(false)
      )
    ) must matchJson("""{"s":"hello!","ni":7,"nf":3.4567,"b":false}""")
  }

  it must "format nested objects" in {
    formatted(
      Seq(
        "obj" -> NestedValue(
          Seq("nested" -> NestedValue(Seq("yes" -> BooleanValue(true), "no" -> BooleanValue(false))))
        )
      )
    ) must matchJson("""{"obj":{"nested":{"yes":true,"no":false}}}""")
  }

  it must "format arrays" in {
    formatted(Seq("array" -> SeqValue(Seq(NumberValue(5.1), NestedValue(Seq("obj" -> StringValue("nested"))))))) must matchJson(
      """{"array":[5.1,{"obj":"nested"}]}"""
    )
  }

  it must "escape strings" in {
    formatted(Seq("hello" -> StringValue("a\tstring\nmust\bbe\rencoded"))) must matchJson(
      """{"hello":"a\tstring\nmust\bbe\rencoded"}"""
    )
  }

  def formatted(values: Seq[(String, Value)]): String =
    new String(format.format(values), StandardCharsets.UTF_8)

  def matchJson(expected: String): Matcher[String] = { str =>
    val expectedJson = PlayJson.parse(expected)
    val givenJson = PlayJson.parse(str)

    MatchResult(
      str.endsWith("\n") && expectedJson == givenJson,
      s"""JSON $givenJson did not match $expectedJson"""",
      s"""JSON $givenJson matched $expectedJson"""
    )
  }

}
