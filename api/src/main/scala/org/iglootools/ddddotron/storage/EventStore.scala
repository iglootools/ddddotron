package org.iglootools.ddddotron.storage

import org.iglootools.ddddotron.core._

/**
 * @see http://blog.jonathanoliver.com/2010/12/cqrs-eventstore-v2-architectural-overview/
 * @see http://historicalmodeling.com/book/
 * Guarantees
 *   Messages will be delivered at least once.
 *   Message duplication will have no ill effects.
 *   Messages will be delivered in the order that they were sent.
 */
trait EventStore extends EventDispatcherStorageSupport with SnapshotStore {

  /**
   * @throws ConcurrencyException
   */
  def attemptCommit[E <: Event](attempt: CommitAttempt[E])

  /**
   * Might kill memory usage if there are too many events. Please use doWithCommittedEvents instead
   */
  def committedEvents(streamType: String, streamId: GUID, fromRevision: Revision = commitRevisionOne): List[CommittedEvent[Event]] = {
    val events = scala.collection.mutable.ListBuffer[CommittedEvent[Event]]()
    doWithCommittedEvents(streamType, streamId, fromRevision) { case committedEvent =>
      events += committedEvent
    }
    events.toList
  }
  def doWithCommittedEvents(streamType: String, streamId: GUID, fromRevision: Revision = commitRevisionOne)(f: CommittedEvent[Event] => Unit)
}