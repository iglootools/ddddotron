package org.iglootools.ddddotron.storage

import org.iglootools.ddddotron.core._
import org.iglootools.ddddotron.ei._

final class Repository[E <: Event, S<:AnyRef, AR <: AggregateRoot[E,S,AR]](val aggregateRootType: String)
                                                                          (implicit aggregateRootFactory: AggregateRootFactory[E,S,AR],
                                                                           eventStore: EventStore,
                                                                           eventBus: EventBus,
                                                                           mfs: Manifest[S],
                                                                           mfe: Manifest[E]) {

  def getById(id: GUID): AR = {
    val snapshot: Option[StreamSnapshot[S]] = eventStore.streamSnapshotOption[S](aggregateRootType, id)
    val fromRevision =  snapshot map (_.includesCommitsUpToRevision + 1) getOrElse(commitRevisionOne)
    val builder = aggregateRootFactory.aggregateRootBuilder(id, snapshot)

    eventStore.doWithCommittedEvents(streamType= aggregateRootType,streamId=id, fromRevision= fromRevision) {
      committedEvent: CommittedEvent[Event] =>
        builder.withEvent(committedEvent.asInstanceOf[CommittedEvent[E]])
    }
    builder.done
  }

  /**
   * Flushes the events accumulated in the given aggregate root and notifies the event bus of them
   *
   * @throws ConcurrencyException if updates to the event store have been done outside of the aggregate root
   */
  def commitPendingEvents(aggregateRoot:AR): AR = {
    aggregateRoot.flushPendingEvents { (commitRevision, pendingEvents) =>
      val commit = CommitAttempt(aggregateRootType, aggregateRoot.aggregateRootId, commitRevision, pendingEvents)
      eventStore.attemptCommit(commit)

      pendingEvents foreach { event =>
        eventBus.publish(event)
      }
      commit.expectedEventRevisions.lastOption
    }
  }

  def persistSnapshot(aggregateRoot: AR) = {
    eventStore.saveStreamSnapshot(
      streamType= aggregateRoot.aggregateRootType,
      streamId=aggregateRoot.aggregateRootId,
      streamSnapshot = aggregateRoot.snapshot
    )
  }
}