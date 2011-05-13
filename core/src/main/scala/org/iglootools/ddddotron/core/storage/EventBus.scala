package org.iglootools.ddddotron.core.storage

import org.iglootools.ddddotron.core._

trait EventBus {
  def publish(event: Event): Unit
}