package com.joprice.protobuf.akka

import com.joprice.protobuf.MacroHelpers
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@compileTimeOnly("enable macro paradise to expand macro annotations")
class akkaSerializer[T](id: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro akkaSerializer.akkaSerializerImpl
}

//TODO: support all types of a desecriptor in one serializer?
object akkaSerializer {

  def akkaSerializerImpl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val (messageClass, id) = c.prefix.tree match {
      case q"new akkaSerializer[$typeArg](${id@Literal(Constant(_))})" =>
        val typeSymbol = c.typecheck(typeArg, mode = c.TYPEmode).symbol
        //TODO: check the type of the type argument - for some reason, the protoClass annotation doesn't show up here
        //val isMessageType = typeSymbol.asType.annotations //<:< weakTypeOf[GeneratedMessage]
        (typeSymbol, id)
      case _ =>
        c.abort(c.enclosingPosition, "Could not find message type")
    }

    //TODO: only some parts of this actually require a macro, namely the case matching, which could also be done with a
    // type tag. Experiment with moving most of this to a class that this macro simply extends
    def serializer(name: TypeName): Tree = q"""
      class $name(val system: _root_.akka.actor.ExtendedActorSystem)
        extends _root_.akka.serialization.SerializerWithStringManifest
        with _root_.akka.serialization.BaseSerializer {

        val toProtobuf = implicitly[_root_.com.joprice.protobuf.ToProtobuf[$messageClass]]
        val fromProtobuf = implicitly[_root_.com.joprice.protobuf.FromProtobuf[$messageClass]]

        def manifest(o: AnyRef): String = o match {
          case _: $messageClass => $id
          case _ =>
            throw new IllegalArgumentException(s"Can't serialize object of type $${o.getClass} in [$${getClass.getName}]")
        }

        override def toBinary(o: AnyRef): Array[Byte] = o match {
          case message: $messageClass =>
            implicitly[_root_.com.joprice.protobuf.ToProtobuf[$messageClass]].toProtobuf(message).toByteArray
          case _ =>
            throw new IllegalArgumentException(s"Can't serialize object of type $${o.getClass}")
        }

        override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
          case $id => implicitly[_root_.com.joprice.protobuf.FromProtobuf[$messageClass]].fromProtobuf(bytes)
          case _ => throw new IllegalArgumentException(s"Can't deserialize class of $$manifest")
        }
      }
    """

    MacroHelpers.replaceAnnotatedClass(c)(annottees) { classDef =>
      List(serializer(classDef.name))
    }
  }
}
