package org.iglootools.ddddotron.core

import org.joda.time.DateTime

trait Event {
  val eventUtcDate: DateTime

  def eventType: String = this.getClass.getSimpleName
  def eventDataVersion: Int = 1
}