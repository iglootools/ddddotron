package org.iglootools.ddddotron.infrastructure.ei

import org.iglootools.ddddotron._

/**
 * An EventBus that discards the events
 */
class NullEventBus extends EventBus {
  def publish(event: Event) {}
}