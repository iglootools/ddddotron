package org.iglootools.ddddotron.infrastructure.serialization

import net.liftweb.json._
import net.liftweb.json.ext.utchacks.JodaTimeSerializers

protected[serialization] object JsonSerializer {
  implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all

}