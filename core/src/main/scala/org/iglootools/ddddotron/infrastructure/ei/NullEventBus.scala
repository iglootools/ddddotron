package org.iglootools.ddddotron.infrastructure.ei

import org.iglootools.ddddotron.core.storage.EventBus
import org.iglootools.ddddotron.core.Event

/**
 * An EventBus that discards the events
 */
class NullEventBus extends EventBus {
  def publish(event: Event) {}
}