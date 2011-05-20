package org.iglootools.ddddotron.core

import scala.Option._

trait AggregateRootFactory[E <:Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]]{
  protected[this] def createAggregateRoot(id: GUID, state: S, commitRevision: Option[Revision], pendingEvents: List[E]): AR
  protected[this] def createInitialState: S

  /**
   * Implies that this is the first event of the aggregate root
   */
  protected[core] def applyEvent(event: E, id: GUID): AR = {
    createAggregateRoot(id, createInitialState, None, List())
      .applyEvent(event)
  }

  def loadFromHistory(aggregateRootId: GUID, eventHistory: EventHistory[E,S]): AR = {
    val aggregateBuilder = aggregateRootBuilder(aggregateRootId, eventHistory.snapshot)
    eventHistory.additionalCommittedEvents foreach (aggregateBuilder.withEvent(_))
    return aggregateBuilder.done
  }

  def aggregateRootBuilder(aggregateRootId: GUID, snapshot: Option[StreamSnapshot[S]]): IncrementalAggregateRootBuilder = {
    new IncrementalAggregateRootBuilder(
          createAggregateRoot(
            aggregateRootId,
            snapshot.map(_.state).getOrElse(createInitialState),
            snapshot.map(_.includesCommitsUpToRevision),
            List()))
  }

  class IncrementalAggregateRootBuilder(aggregateRoot: AR) {
    var currentAR = aggregateRoot
    var currentRevision = aggregateRoot.commitRevision
    def withEvent(committedEvent: CommittedEvent[E]) {
      currentAR = currentAR.applyEvent(committedEvent.event)
      currentRevision = Some(committedEvent.commitRevision)

    }
    def done = {
      currentAR.flushPendingEvents{ case (commitRevision, events) =>
        currentRevision
      }
    }
  }
}

