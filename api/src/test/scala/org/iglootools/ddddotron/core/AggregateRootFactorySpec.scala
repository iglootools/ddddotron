package org.iglootools.ddddotron.core

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.testdomain._
import org.scalatest.Spec
import org.joda.time.DateTime
import EventHistories._

@RunWith(classOf[JUnitRunner])
class AggregateRootFactorySpec extends Spec with ShouldMatchers {

  describe("Aggregate Root Factory") {
    it("should apply events when creating object") {
      val aggregateRoot = SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List(event1,event2)))

      aggregateRoot.state.event1Applied should be === true
      aggregateRoot.state.event2Applied should be === true
    }

    it("should create default initial state when no snapshot given") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory())
        .state should be === SomeAggregateRootState()
    }

    it("should restore state when snapshot given") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List()))
        .state should be === snapshotAfterEvent1.state
    }

    it("should restore state and apply events") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List(event2)))
        .state should be === SomeAggregateRootState(event1Applied = true, event2Applied = true)
    }

    it("should not include the initial events in the pending events") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List(event2)))
        .pendingEvents should be === List()
    }

    it("should provide the pending events ") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List(event2)))
        .acceptSomeCommand
        .acceptAnotherCommand
        .pendingEvents match {
          case List((SomeCommandAccepted(_)),(AnotherCommandAccepted(_))) => // event1 (snapshot) + event2 (history) + 2 events
          case obj => fail("Pending Events does not match expected value: " + obj)
        }
    }
  }

  def event1 = MyEvent("1-data")
  def event2 = MyEvent("2-data")

  def snapshotAfterEvent1 = StreamSnapshot(state=SomeAggregateRootState(event1Applied = true),includesCommitsUpToRevision=1)

}