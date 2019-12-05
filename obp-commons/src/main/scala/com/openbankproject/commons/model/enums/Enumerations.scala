package com.openbankproject.commons.model.enums

import com.openbankproject.commons.util.{EnumValue, OBPEnumeration}
import net.liftweb.json.{JArray, JBool, JDouble, JInt, JObject, JValue}
import net.liftweb.json.JsonAST.JString

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

sealed trait AccountAttributeType extends EnumValue
object AccountAttributeType extends OBPEnumeration[AccountAttributeType]{
  object STRING         extends Value
  object INTEGER        extends Value
  object DOUBLE         extends Value
  object DATE_WITH_DAY  extends Value
}

sealed trait ProductAttributeType extends EnumValue
object ProductAttributeType extends OBPEnumeration[ProductAttributeType]{
  object STRING        extends Value
  object INTEGER       extends Value
  object DOUBLE        extends Value
  object DATE_WITH_DAY extends Value
}

sealed trait CardAttributeType extends EnumValue
object CardAttributeType extends  OBPEnumeration[CardAttributeType]{
  object STRING        extends Value
  object INTEGER       extends Value
  object DOUBLE        extends Value
  object DATE_WITH_DAY extends Value
}

//------api enumerations ----
sealed trait StrongCustomerAuthentication extends EnumValue
object StrongCustomerAuthentication extends OBPEnumeration[StrongCustomerAuthentication] {
  type SCA = Value
  object SMS extends Value
  object EMAIL extends Value
  object DUMMY extends Value
  object UNDEFINED extends Value
}

sealed trait PemCertificateRole extends EnumValue
object PemCertificateRole extends OBPEnumeration[PemCertificateRole] {
  type ROLE = Value
  object PSP_AS extends Value
  object PSP_IC extends Value
  object PSP_AI extends Value
  object PSP_PI extends Value
}
//------api enumerations end ----

sealed trait DynamicEntityFieldType extends EnumValue {
  val jValueType: Class[_]
  def isJValueValid(jValue: JValue): Boolean = jValueType.isInstance(jValue)
}
object DynamicEntityFieldType extends OBPEnumeration[DynamicEntityFieldType]{
 object string  extends Value{val jValueType = classOf[JString]}
 object number  extends Value{val jValueType = classOf[JDouble]}
 object integer extends Value{val jValueType = classOf[JInt]}
 object boolean extends Value{val jValueType = classOf[JBool]}
 //object array extends Value{val jValueType = classOf[JArray]}
 //object `object` extends Value{val jValueType = classOf[JObject]} //TODO in the future, we consider support nested type
}

/**
 * connector support operation type for DynamicEntity
 */
sealed trait DynamicEntityOperation extends EnumValue
object DynamicEntityOperation extends OBPEnumeration[DynamicEntityOperation] {
  object GET_ALL extends Value
  object GET_ONE extends Value
  object CREATE extends Value
  object UPDATE extends Value
  object DELETE extends Value
  object IS_EXISTS_DATA extends Value
}