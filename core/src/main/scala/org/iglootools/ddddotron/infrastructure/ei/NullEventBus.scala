package org.iglootools.ddddotron.infrastructure.ei

import org.iglootools.ddddotron.core.Event
import org.iglootools.ddddotron.ei._

/**
 * An EventBus that discards the events
 */
class NullEventBus extends EventBus {
  def publish(event: Event) {}
}