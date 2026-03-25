/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sbt.journey.models

/** The type of user answer.
  */
sealed abstract class FieldType extends Product with Serializable {
  def typeName: Option[String] = this match {
    case ClassType(clazz) => clazz.split("\\.").lastOption
    case _                => None
  }
}

object FieldType {
  val BYTE    = PrimitiveType(classOf[Byte])
  val SHORT   = PrimitiveType(classOf[Short])
  val INT     = PrimitiveType(classOf[Int])
  val LONG    = PrimitiveType(classOf[Long])
  val FLOAT   = PrimitiveType(classOf[Float])
  val DOUBLE  = PrimitiveType(classOf[Double])
  val CHAR    = PrimitiveType(classOf[Char])
  val BOOLEAN = PrimitiveType(classOf[Boolean])
  val STRING  = ClassType(classOf[String].getName)
}

/** An array answer.
  * @param elements
  *   the element type of the array.
  */
case class ArrayType(elements: FieldType) extends FieldType

/** A list answer.
  * @param elements
  *   the element type of the list.
  */
case class ListType(elements: FieldType) extends FieldType

/** An optional answer.
  * @param elements
  *   the element type of the option.
  */
case class OptionType(elements: FieldType) extends FieldType

/** A set answer.
  * @param elements
  *   the element type of the set.
  */
case class SetType(elements: FieldType) extends FieldType

/** A map answer.
  * @param keys
  *   the key type of the map.
  * @param values
  *   the value type of the map.
  */
case class MapType(keys: FieldType, values: FieldType) extends FieldType

/** A primitive type answer.
  * @param clazz
  *   A class reference for the primitive type.
  */
case class PrimitiveType(clazz: Class[? <: AnyVal]) extends FieldType

/** A class type answer.
  * @param clazz
  *   A fully qualified class name.
  */
case class ClassType(clazz: String) extends FieldType

object ClassType {
  def apply(name: QualifiedName): ClassType =
    ClassType(name.toString)
}

/** A synthetic class type answer. These are classes that are generated to represent user answers
  * according to the declared journey structure.
  * @param clazz
  *   A fully qualified class name.
  */
case class SyntheticClassType(pkg: String, name: String) extends FieldType
