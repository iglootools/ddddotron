package org.iglootools.ddddotron.core;

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
    var aggregate = createAggregateRoot(aggregateRootId, eventHistory.state.getOrElse(createInitialState), eventHistory.commitRevision, List())
    for (event <- eventHistory.additionalEvents)
      aggregate = aggregate.applyEvent(event)
    return aggregate.flushPendingEvents { case (commitRevision, events) =>
      eventHistory.commitRevision
    }
  }
}