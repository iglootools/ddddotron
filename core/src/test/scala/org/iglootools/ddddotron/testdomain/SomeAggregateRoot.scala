package org.iglootools.ddddotron.testdomain

import org.iglootools.ddddotron._
import core.Event
import org.iglootools.commons.ddd.{DDDFactory, DDDAggregateRoot}

@DDDFactory
object SomeAggregateRoot extends AggregateRootFactory[Event, SomeAggregateRootState, SomeAggregateRoot]{
  def apply(id: GUID) = applyEvent(SomethingCreated(), id)

  protected[this] def createAggregateRoot(aggregateRootId: GUID, state: SomeAggregateRootState, commitRevision: Option[Revision], pendingEvents: List[Event]): SomeAggregateRoot = {
    new SomeAggregateRoot(aggregateRootId=aggregateRootId, state=state, commitRevision=commitRevision, pendingEvents = pendingEvents)
  }

  protected[this] def createInitialState: SomeAggregateRootState = SomeAggregateRootState()
}

@DDDAggregateRoot
final class SomeAggregateRoot(val aggregateRootId: GUID,
                              val state: SomeAggregateRootState,
                              val commitRevision: Option[Revision]=None,
                              val pendingEvents: List[Event]=List())
  extends AggregateRoot[Event, SomeAggregateRootState, SomeAggregateRoot] {

  def acceptSomeCommand = {
    applyEvent(SomeCommandAccepted())
  }

  def acceptAnotherCommand = {
    applyEvent(AnotherCommandAccepted())
  }

  // Event Handlers
  protected[this] val handleEvent = handleSomethingCreated orElse handleSomeCommandAccepted orElse handleAnotherCommandAccepted orElse handleMyEvent

  private def handleSomethingCreated = handler {event: SomethingCreated =>
    state.copy()
  }

  private def handleSomeCommandAccepted = handler {event: SomeCommandAccepted =>
    state.copy(someCommandAccepted = true)
  }

  private def handleAnotherCommandAccepted = handler {event: AnotherCommandAccepted =>
    state.copy(anotherCommandAccepted = true)
  }

  private def handleMyEvent = handler { event: MyEvent =>
    event match {
      case MyEvent(data, _) if data == "1-data" => state.copy(event1Applied = true)
      case MyEvent(data, _) if data == "2-data" => state.copy(event2Applied = true)
    }
  }


  def copy(aggregateRootId: GUID, state: SomeAggregateRootState, commitRevision: Option[Revision], pendingEvents: List[Event]): SomeAggregateRoot = {
    new SomeAggregateRoot(aggregateRootId, state, commitRevision, pendingEvents)
  }

  def aggregateRootType: String = "MyAggregate"
}