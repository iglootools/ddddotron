package org.iglootools.ddddotron.core

trait EventSourced[E <: Event, ES <: EventSourced[E,ES]] {
  protected[core] def applyEvent(event: E): ES
  protected[this] def unhandled(event: E) = error("Event " + event + "cannot be applied")
}