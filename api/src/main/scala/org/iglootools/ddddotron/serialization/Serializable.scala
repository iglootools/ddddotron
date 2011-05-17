package org.iglootools.ddddotron.serialization

import org.iglootools.commons.scala._
import org.iglootools.ddddotron.core._
import net.liftweb.json.Serialization.{read, write}
import grizzled.slf4j.Logging
import net.liftweb.json._

protected[ddddotron] case class Serializable[T <: Event]()(implicit mf: Manifest[T]) extends Logging {
  def supportedType: Class[_] = mf.erasure
  def supportedTypeKey: String = supportedType.getSimpleName

  def deserialize(data: String)(implicit formats: Formats): T = {
    parse(data).extract[T]
  }

  def serialize(event: Event)(implicit formats: Formats): String = {
    returning(write(event)) { s =>
      debug("Serialized event " + event + " to: " + s)
    }
  }
}

