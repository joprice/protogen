package com.joprice.protobuf

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

  implicit def genToProtobuf[T]: ToProtobuf[T] = macro genToProtobufImpl[T]

  def genToProtobufImpl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[ToProtobuf[T]] = {
    import c.universe._

    val caseClassType = weakTypeOf[T]
    val messageClass = MacroHelpers.getMessageClass[T](c)
    val messageDescriptor = MacroHelpers.descriptor(c)(messageClass)

    val setters = messageDescriptor.getFields.asScala.map { field =>
      val name = field.getName
      val caseClassField = q"t.${TermName(name)}"
      val setter = TermName(s"set${name.capitalize}")
      if (field.isOptional)
        q"$caseClassField.foreach(b.$setter(_))"
      else
        //TODO: parameterize prefix 'b'
        q"b.$setter($caseClassField)"
    }

    //println(show(setters))

    val tree = q"""
        new _root_.com.joprice.protobuf.ToProtobuf[$caseClassType] {
          type Out = $messageClass
          def toProtobuf(t: $caseClassType): Out = {
            val b = ${messageClass.companion}.newBuilder
            ..$setters
            b.build
          }
        }
      """
    c.Expr[ToProtobuf[T]](tree)
  }

}


