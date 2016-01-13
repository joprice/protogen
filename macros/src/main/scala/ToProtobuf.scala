package com.joprice.protobuf

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.GeneratedMessage
import scala.reflect.macros.blackbox
import scala.language.experimental.macros
import scala.collection.JavaConverters._

trait ToProtobuf[T] {
  type Out <: GeneratedMessage
  def toProtobuf(t: T): Out
}


object ToProtobuf {
  def apply[T](t: T)(implicit tp: ToProtobuf[T]): tp.Out = tp.toProtobuf(t)

  //TODO: move to package object
  implicit class RichToProtobuf[T](val t: T) extends AnyVal {
    def toProtobuf(implicit tp: ToProtobuf[T]) = ToProtobuf(t)
  }

  implicit def genToProtobuf[T]: ToProtobuf[T] = macro genToProtobufImpl[T]

  def genToProtobufImpl[T](c: blackbox.Context)(
    implicit wt: c.WeakTypeTag[T]
  ): c.Expr[ToProtobuf[T]] = {
    import c.universe._

    val annotation = wt.tpe.typeSymbol.asType.annotations
      .find(_.tree.tpe <:< weakTypeOf[protoClass[_]]).getOrElse {
      c.abort(c.enclosingPosition,
        s"""|Did not find @protoClass annotation on ${wt.tpe}. Add the annotation with the protobuf message type,
            |or use @proto annotation to generate the class from the protobuf message class.
            |""".stripMargin
      )
    }
    val messageClass = annotation.tree.tpe.typeArgs.head
    val messageClassCompanion = messageClass.companion
    val messageDescriptor = c.eval(c.Expr[Descriptor](q"$messageClassCompanion.getDescriptor()"))
    val setters = messageDescriptor.getFields.asScala.map { field =>
      val name = field.getName
      //TODO: generate foreach set on option
      if (field.isOptional) {
        val caseClassField = q"t.${TermName(name)}"
        q"""$caseClassField.foreach { value =>
          b.${TermName(s"set${name.capitalize}")}(value)
        }"""
      } else {
        q"b.${TermName(s"set${name.capitalize}")}(t.${TermName(name)})"
      }
    }

    //println(show(setters))

    val tree = q"""
        new com.joprice.protobuf.ToProtobuf[${wt.tpe}] {
          type Out = $messageClass
          def toProtobuf(t: ${wt.tpe}): Out = {
            val b = $messageClassCompanion.newBuilder
            ..$setters
            b.build
          }
        }
      """
    c.Expr[ToProtobuf[T]](tree)
  }

}

//trait FromProtobuf[T] {
//  type Out <: GeneratedMessage
//  def fromProtobuf(t: Array[Byte]): T
//}
//
//object FromProtobuf {
//
//  def apply[T](t: Array[Byte])(implicit tp: FromProtobuf[T]): T = tp.fromProtobuf(t)
//
//  //TODO: move to package object
//  implicit class RichFromProtobuf(val data: Array[Byte]) extends AnyVal {
//    def fromProtobuf[T](implicit tp: FromProtobuf[T]) = FromProtobuf(data)
//  }
//
//  implicit def genFromProtobuf[T]: FromProtobuf[T] = macro genFromProtobufImpl[T]
//
//  def genFromProtobufImpl[T](c: blackbox.Context)(
//    implicit wt: c.WeakTypeTag[T]
//  ): c.Expr[FromProtobuf[T]] = {
//    import c.universe._
//    c.Expr[FromProtobuf[T]](reify(null))
//  }
//
//}

