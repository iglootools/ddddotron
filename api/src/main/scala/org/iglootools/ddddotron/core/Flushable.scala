package org.iglootools.ddddotron.core

trait Flushable[E <: Event, S<:AnyRef, AR <: AggregateRoot[E, S, AR]] {
  self: Copyable[E, S, AR] =>

  def pendingEvents: List[E]
  def commitRevision: Option[Revision]

  def flushPendingEvents(f: ((Option[Revision], List[E]) => Option[Revision])): AR = {
    val nextRevision = f(commitRevision, self.pendingEvents)
    copy(commitRevision=nextRevision, pendingEvents=Nil)
  }

}
