package org.iglootools.ddddotron.core

trait AggregateRoot[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]]
  extends EventSourced[E, AR]
  with Snapshottable[E,S,AR]
  with Flushable[E,S, AR]
  with Copyable[E,S,AR]
  with FunctionalEventHandling
  with AggregateRootIdentityProvider {

  protected[core] def applyEvent(event: E): AR = {
    copy(state=handleEventOrRaiseError(event), pendingEvents=pendingEvents :+ event)
  }

  protected[this] def handleEventOrRaiseError(event: E): S = {
    if(handleEvent.isDefinedAt(event))
      handleEvent(event)
    else
      unhandled(event)
  }

  /**
   * S is the new state after handling the given event
   */
  protected[this] val handleEvent: PartialFunction[E, S]



}