package com.joprice.protobuf

import com.google.protobuf.GeneratedMessage
import scala.reflect.macros.blackbox
import scala.language.experimental.macros
import scala.collection.JavaConverters._

trait FromProtobuf[T] {
  type Out <: GeneratedMessage
  def fromProtobuf(data: Array[Byte]): T
}

object FromProtobuf {

  def apply[T](t: Array[Byte])(implicit tp: FromProtobuf[T]): T = tp.fromProtobuf(t)

  implicit def genFromProtobuf[T]: FromProtobuf[T] = macro genFromProtobufImpl[T]

  def genFromProtobufImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[FromProtobuf[T]] = {
    import c.universe._
    val messageClass = MacroHelpers.getMessageClass[T](c)
    val messageDescriptor = MacroHelpers.descriptor(c)(messageClass)

    val getters = messageDescriptor.getFields.asScala.map { field =>
      val name = field.getName
      val select = q"message.${TermName(s"get${name.capitalize}")}"
      val wrapped = if (field.isOptional) {
        q"(if (message.${TermName(s"has${name.capitalize}")}) Option($select) else None)"
      } else select
      q"${TermName(name)} = $wrapped"
    }

    val caseClassType = weakTypeOf[T]
    val tree = q"""
      new _root_.com.joprice.protobuf.FromProtobuf[$caseClassType] {
        type Out = $messageClass
        def fromProtobuf(data: Array[Byte]): $caseClassType = {
          val message = ${messageClass.companion}.parseFrom(data)
          new $caseClassType(..$getters)
        }
      }
    """

    // println(show(tree))

    c.Expr[FromProtobuf[T]](tree)
  }

}
