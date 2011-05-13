package org.iglootools.ddddotron.infrastructure.serialization

import org.iglootools.commons.scala._
import org.iglootools.ddddotron.core._
import org.iglootools.ddddotron.serialization._
import org.iglootools.commons.scala.handlerregistry.{HandlerTypeRegistry, HandlerRegistry}
import net.liftweb.json._

protected[ddddotron] class JsonEventSerializer(hints: List[Serializable[_]], additionalSerializers: List[Serializer[_]]=List()) extends EventSerializer{
  implicit private[this] val formats = JsonSerializer.formats ++ additionalSerializers

  val serializers: HandlerTypeRegistry[Serializable[_]] = new HandlerTypeRegistry(hints, { s: Serializable[_] => s.supportedType })
  val deserializers: HandlerRegistry[String, Serializable[_]] = new HandlerRegistry(hints, {s: Serializable[_] => s.supportedTypeKey })

  def deserialize(eventType: String, data: String): Event = deserializers.handlerFor(eventType).deserialize(data).asInstanceOf[Event]
  def serialize[E <: Event](event: E): String = serializers.handlerFor(event.getClass).serialize(event)
}

