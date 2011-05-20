package org.iglootools.ddddotron.core

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
    val aggregateBuilder = new IncrementalAggregateRootBuilder(
      createAggregateRoot(
        aggregateRootId,
        eventHistory.state.getOrElse(createInitialState),
        eventHistory.snapshotCommitRevision,
        List()))

    eventHistory.additionalCommittedEvents foreach (aggregateBuilder.withEvent(_))
    return aggregateBuilder.done
  }

  class IncrementalAggregateRootBuilder(aggregateRoot: AR) {
    var currentAR = aggregateRoot
    var currentRevision = aggregateRoot.commitRevision
    def withEvent(committedEvent: CommittedEvent[E]) {
      currentAR = currentAR.applyEvent(committedEvent.event)
      currentRevision = Some(committedEvent.commitRevision)greg

    }
    def done = {
      currentAR.flushPendingEvents{ case (commitRevision, events) =>
        currentRevision
      }
    }
  }
}

