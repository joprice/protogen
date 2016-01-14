package com.joprice.protobuf

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.GeneratedMessage
import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.reflect.macros.blackbox
import scala.language.experimental.macros
import scala.collection.JavaConverters._
import com.google.protobuf.Descriptors.FieldDescriptor

// annotation that replaces original proto annotation, to tag with the descriptor class, but avoid recursive
// recompilation of annotation macro
class protoClass[T] extends StaticAnnotation

@compileTimeOnly("enable macro paradise to expand macro annotations")
class proto[T] extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro proto.protoImpl
}

object proto {

  def scalaType(field: FieldDescriptor) = {
    import JavaType._
    field.getJavaType match {
      case INT => "Int"
      case STRING => "String"
      case LONG => "Long"
      case FLOAT => "Float"
      case DOUBLE => "Double"
      case BOOLEAN => "Boolean"
      //case BYTE_STRING,
      //ENUM,
      //MESSAGE;
    }
  }

  //TODO: possible to disallow multiple of same annotation?
  def protoImpl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val fieldToParameter = { (field: FieldDescriptor) =>
      val baseType = tq"${TypeName(scalaType(field))}"
      val fieldType = if (field.isOptional) tq"Option[$baseType]" else baseType
      q"val ${TermName(field.getName)}: $fieldType"
    }

    def generateCaseClass(messageClass: Symbol, name: TypeName) = {
      val descriptor = c.eval(c.Expr[Descriptor](q"${messageClass.companion}.getDescriptor()"))
      val methods = descriptor.getFields.asScala.toList.map(fieldToParameter)
      // tagging this with original protobuf message class for use later by implicit ToProtobuf macro
      q"@_root_.com.joprice.protobuf.protoClass[$messageClass] case class $name(..$methods)"
    }

    def generateClass(messageClass: Symbol) = {
      def caseClass(clazz: ClassDef) = List(generateCaseClass(messageClass, clazz.name))

      val members = annottees.map(_.tree).toList match {
        case (clazz: ClassDef) :: companion :: Nil =>
          //TODO: required to merge case class trees with existing companion?
          caseClass(clazz) ++ List(companion)
        case (clazz: ClassDef) :: Nil =>
          caseClass(clazz)
        case _ =>
          c.abort(c.enclosingPosition, "proto annotation should be used on a class")
      }

      c.Expr[Any](Block(members, Literal(Constant(()))))
    }

    c.prefix.tree match {
      case q"new proto[$typeArg]" =>
        val messageClass = c.typecheck(typeArg, mode = c.TYPEmode).symbol
        val isMessageType = messageClass.asType.toType <:< weakTypeOf[GeneratedMessage]

        if (!isMessageType) {
          val parentClass = classOf[GeneratedMessage].getCanonicalName
          c.abort(c.enclosingPosition, s"$messageClass must be a subtype of $parentClass")
        } else {
          generateClass(messageClass)
        }
      case _ =>
        c.abort(c.enclosingPosition, "proto expects a single type that is a subclass of GeneratedMessage")
    }
  }
}
