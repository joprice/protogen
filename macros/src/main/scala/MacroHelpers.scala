package com.joprice.protobuf

import com.google.protobuf.Descriptors.Descriptor

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[protobuf] object MacroHelpers {

  def descriptor(c: blackbox.Context)(messageClass: c.Type) = {
    import c.universe._
    val messageClassCompanion = messageClass.companion
    c.eval(c.Expr[Descriptor](q"$messageClassCompanion.getDescriptor()"))
  }

  def getMessageClass[T: c.WeakTypeTag](c: blackbox.Context): c.Type = {
    import c.universe._
    val wt = weakTypeOf[T]
    val annotation = wt.typeSymbol.asType.annotations
      .find(_.tree.tpe <:< weakTypeOf[protoClass[_]]).getOrElse {
      c.abort(c.enclosingPosition,
        s"""|Did not find @protoClass annotation on $wt. Add the annotation with the protobuf message type,
            |or use @proto annotation to generate the class from the protobuf message class.
            |""".stripMargin
      )
    }
    annotation.tree.tpe.typeArgs.head
  }

}
