package org.iglootools.ddddotron.infrastructure.serialization

import org.iglootools.ddddotron.core._
import org.iglootools.ddddotron.serialization.StreamStateSerializer
import org.iglootools.commons.scala.handlerregistry.{HandlerTypeRegistry, HandlerRegistry}
import net.liftweb.json._
import net.liftweb.json.Serialization._


protected[ddddotron] class JsonAggregateRootStateSerializer(additionalSerializers: List[Serializer[_]]=List()) extends StreamStateSerializer{
  implicit private[this] val formats = JsonSerializer.formats ++ additionalSerializers

  def deserialize[S](data: String)(implicit mf: Manifest[S]): S = parse(data).extract[S]
  def serialize[S <: AnyRef](state: S): String = write(state)
}

