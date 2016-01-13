package com.joprice.protobuf

import messages.Messages.SampleMessage
import com.joprice.protobuf.Macros.proto
import shapeless._
import scala.language.experimental.macros

import scala.collection.JavaConverters._

@proto[SampleMessage]
class Message
object Message {
  //def apply(i: Int) = new Message(id = i, name = "testing")
}

object Main {
  def main(args: Array[String]): Unit = {
    val m = Message(id = 1, name = "abc")
    println(m)
  }
}


