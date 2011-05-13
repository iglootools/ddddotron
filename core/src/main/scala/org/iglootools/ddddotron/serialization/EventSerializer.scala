package org.iglootools.ddddotron.serialization

import org.iglootools.ddddotron.core._

protected[ddddotron] trait EventSerializer {
  def serialize[E <: Event](event: E): String
  def deserialize(eventType: String, data: String): Event
}