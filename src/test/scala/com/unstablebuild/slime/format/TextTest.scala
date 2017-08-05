package com.unstablebuild.slime.format

import java.nio.charset.StandardCharsets

import com.unstablebuild.slime._
import org.scalatest.{FlatSpec, MustMatchers}

class TextTest extends FlatSpec with MustMatchers {

  it must "format values as text" in {

    val format = new Text
    format.setSeparator(",")

    val output = new String(
      format.format(
        Seq(
          "str" -> StringValue("abc"),
          "num" -> NumberValue(3.14),
          "bool" -> BooleanValue(false),
          "seq" -> SeqValue(Seq(NumberValue(1), NumberValue(2))),
          "obj" -> NestedValue(Seq("other" -> StringValue("xyz"))),
          "objs" -> SeqValue(Seq(NestedValue(Seq("other" -> StringValue("xyz")))))
        )
      ),
      StandardCharsets.UTF_8
    )

    output must equal("str=abc,num=3.14,bool=false,seq=[1,2],obj.other=xyz,objs=[{.other=xyz}]\n")
  }

}
