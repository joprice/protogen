package com.joprice.protobuf

import messages.Messages.SampleMessage
import scala.language.experimental.macros
import org.scalatest._

class ProtoSpec extends FlatSpec with Matchers {
  class A

  //@proto[A]
  @proto[SampleMessage]
  class Message

  "the proto annotation" should "allow an object to be serialized and deserialized" in {
    val original = Message(id = Option(1), name = "abc")
    val protobuf = original.toProtobuf
    val parsed = protobuf.toByteArray.fromProtobuf[Message]
    assert(parsed == original)
  }

  it should "handle options" in {
    //SampleMessage.ID_FIELD_NUMBER
    val original = Message(id = None, name = "abc")
    val protobuf = original.toProtobuf
    val parsed = protobuf.toByteArray.fromProtobuf[Message]
    assert(parsed == original)
  }

}

