package com.joprice.protobuf

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.Descriptor
import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.reflect.macros.blackbox
import scala.language.experimental.macros
import scala.collection.JavaConverters._

object Macros {

  @compileTimeOnly("enable macro paradise to expand macro annotations")
  class proto[T] extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro proto.impl
  }

  object proto {

    def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._

      c.prefix.tree match {
        case q"new proto[$typeArgs]" =>
          val tpe = c.typecheck(typeArgs, mode = c.TYPEmode).symbol.typeSignature
          val clazz = tpe.companion
          val d = c.eval(c.Expr[Descriptor](q"$clazz.getDescriptor()"))
          //TODO: check subclass of GeneratedMessage
          val methods = d.getFields.asScala.map { f =>
            val n = f.getName
            val o = f.isOptional
            import JavaType._
            val t = f.getJavaType match {
              case INT => "Int"
              case STRING => "String"
              //case LONG =>, case FLOAT =>, DOUBLE, BOOLEAN, STRING, BYTE_STRING, ENUM, MESSAGE;
            }
            q"val ${TermName(n)}: ${TypeName(t)}"
          }.toList

          val inputs = annottees.map(_.tree).toList
          def error = c.abort(c.enclosingPosition, "proto annotation should be used on a class")

          def generate(name: TypeName) = q"case class $name(..$methods)"

          //TODO: generate builder calls in companion
          //println(SampleMessage.newBuilder.setId(123).build)

          inputs match {
            case (clazz: ClassDef) :: companion :: Nil =>
              //TODO: required to merge case class trees with existing companion?
              c.Expr[Any](Block(List(generate(clazz.name), companion), Literal(Constant(()))))
            case (clazz: ClassDef) :: Nil =>
              c.Expr[Any](Block(List(generate(clazz.name)), Literal(Constant(()))))
            case _ =>
              error
          }
        case _ =>
          c.abort(c.enclosingPosition, "proto expects a single type that is a subclass of GeneratedMessage")
      }
    }
  }

}
