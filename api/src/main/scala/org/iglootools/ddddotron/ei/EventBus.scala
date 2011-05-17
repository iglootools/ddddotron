package org.iglootools.ddddotron.ei

import org.iglootools.ddddotron.core._

trait EventBus {
  def publish(event: Event): Unit
}