package org.iglootools.ddddotron.core

/**
 * Contains everything needed to reconstruct an Aggregate Root from data stored in the Event Store (Last Snapshot + delta events)
 */
final case class EventHistory[+E <: Event, +S <: AnyRef](val snapshot: Option[StreamSnapshot[S]]=None, val additionalCommittedEvents: List[CommittedEvent[E]]=List()) {
  require(snapshot != null, "snapshot is required")
  require(additionalCommittedEvents != null, "events are required")

  def state: Option[S] = snapshot map (_.state)

  def commitRevision: Option[Revision] = {
    val lastCommitRevision: Option[Revision] = additionalCommittedEvents.lastOption.map(_.commitRevision)
    lastCommitRevision.orElse(snapshot map (_.includesCommitsUpToRevision))
  }

  def additionalEvents: List[E] = additionalCommittedEvents map (_.event)

}