package com.joprice.protobuf

import messages.Messages.SampleMessage
import scala.language.experimental.macros
import ToProtobuf._

class A

//@proto[A]
@proto[SampleMessage]
class Message

object Main {
  def main(args: Array[String]): Unit = {
    val m = Message(id = Option(1), name = "abc").toProtobuf
    println(m)
  }
}


