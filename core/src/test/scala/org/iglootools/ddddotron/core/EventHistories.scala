package org.iglootools.ddddotron.core

import org.iglootools.ddddotron.storage.Locking

object EventHistories {
  def eventHistory[E <:Event, S <: AnyRef](snapshot: Option[StreamSnapshot[S]]=None, additionalEvents: List[E]=List()): EventHistory[E,S] = {
    val commitRevision = snapshot map (_.includesCommitsUpToRevision)
    val eventsWithRevision = additionalEvents zip Locking.expectedRevisions(commitRevision, additionalEvents.size)
    val committedEvents = eventsWithRevision map { case (e, r) => CommittedEvent("MyAggregateRoot", "myId", r, e) }
    EventHistory(snapshot, committedEvents)
  }


}