package org.iglootools.ddddotron.core

import org.joda.time.DateTime

trait Event {
  val timestamp: DateTime

  def eventType: String = this.getClass.getSimpleName
  def version: Int = 1
}