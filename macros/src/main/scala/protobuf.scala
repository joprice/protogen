package com.joprice

package object protobuf {

  //TODO: move to package object
  implicit class RichToProtobuf[T](val t: T) extends AnyVal {
    def toProtobuf(implicit tp: ToProtobuf[T]) = ToProtobuf(t)
  }

  //TODO: move to package object
  implicit class RichFromProtobuf(val data: Array[Byte]) extends AnyVal {
    def fromProtobuf[T](implicit tp: FromProtobuf[T]) = FromProtobuf(data)
  }

}
