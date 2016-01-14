package com.joprice.protobuf

import akka.actor.{ExtendedActorSystem, ActorSystem}
import akka.serialization.{BaseSerializer, SerializationExtension}
import com.typesafe.config.ConfigFactory
import messages.Messages.SampleMessage
import scala.language.experimental.macros
import org.scalatest._

@proto[SampleMessage]
class Message

//TODO: macro implementation
class MessageSerializer(val system: ExtendedActorSystem) extends BaseSerializer {
  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case message: Message =>
      implicitly[ToProtobuf[Message]].toProtobuf(message).toByteArray
    case _ =>
      throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass}")
  }

  val MessageClass = classOf[Message]

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = manifest match {
    case Some(MessageClass) => implicitly[FromProtobuf[Message]].fromProtobuf(bytes)
    case _ => throw new IllegalArgumentException(s"Can't deserialze class of $manifest")

  }
}

class AkkaSerializationSpec extends FlatSpec with Matchers {

  //TODO: akka-testkit
  "the proto annotation" should "work with akka serialization" in {
    val config = ConfigFactory.parseString("""
      |akka {
      |  actor {
      |    serialization-identifiers {
      |      "com.joprice.protobuf.MessageSerializer" = 85
      |    }
      |
      |    serializers {
      |      messageSerializer = "com.joprice.protobuf.MessageSerializer"
      |    }
      |
      |    serialization-bindings {
      |      "com.joprice.protobuf.Message" = messageSerializer
      |    }
      |  }
      |}
      |""".stripMargin).withFallback(ConfigFactory.load())
    val system = ActorSystem("serialization", config)

    val s = SerializationExtension(system)

    val original = Message(id = Option(1), name = "abc")
    s.deserialize(s.serialize(original).get, classOf[Message])

    system.shutdown()
  }

}

