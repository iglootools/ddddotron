package org.iglootools.ddddotron.core

/**
 * TODO: we could use reflection instead because aggregate root constructor parameters are fixed
 */
trait Copyable[E <: Event, S<:AnyRef, AR <: AggregateRoot[E, S, AR]] {
  self: AggregateRootIdentityProvider with Flushable[E,S,AR] with Snapshottable[E,S,AR] =>

  def copy(aggregateRootId: GUID=self.aggregateRootId,
           state: S=self.state,
           commitRevision: Option[Revision]=self.commitRevision,
           pendingEvents: List[E]=self.pendingEvents): AR

}
