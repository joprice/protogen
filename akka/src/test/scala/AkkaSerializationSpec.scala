package com.joprice.protobuf.akka

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.{TestKit, ImplicitSender}
import com.joprice.protobuf.proto
import com.typesafe.config.ConfigFactory
import messages.Messages.SampleMessage
import scala.language.experimental.macros
import scala.concurrent.duration._
import org.scalatest._

@proto[SampleMessage]
class Message

@akkaSerializer[Message]("message")
class MessageSerializer

class AkkaSerializationSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = {
    //TODO: akka-testkit
    this(ActorSystem("serialization",
      ConfigFactory.parseString("""
       |akka {
       |  actor {
       |    serialization-identifiers {
       |      "com.joprice.protobuf.akka.MessageSerializer" = 85
       |    }
       |
       |    serializers {
       |      messageSerializer = "com.joprice.protobuf.akka.MessageSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "com.joprice.protobuf.akka.Message" = messageSerializer
       |    }
       |  }
       |}
       |""".stripMargin
      ).withFallback(ConfigFactory.load())
    ))
  }

  override def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  "the proto annotation" should "work with akka serialization" in {
    val s = SerializationExtension(system)

    val original = Message(id = Option(1), name = "abc")
    original === s.deserialize(s.serialize(original).get, classOf[Message])
  }

}

