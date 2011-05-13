package org.iglootools.ddddotron.core

trait Snapshottable [E <: Event, S<:AnyRef, AR <: AggregateRoot[E, S, AR]] {
  self: Flushable[E,S,AR] =>

  def state: S

  def snapshot: StreamSnapshot[S] = {
    assume(self.pendingEvents.size == 0, "Snapshot: cannot process as there are still %s pending events: %s".format(pendingEvents.size, pendingEvents))
    assume(self.commitRevision.isDefined, "There are no event to snapshot")

    StreamSnapshot(state=state,includesCommitsUpToRevision=self.commitRevision.get)
  }

}