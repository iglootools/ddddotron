package org.iglootools.ddddotron.core

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.{JUnitRunner}
import org.junit.runner.RunWith
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.testdomain._
import org.scalatest.{BeforeAndAfterEach, Spec}
import org.joda.time.DateTime
import EventHistories._

@RunWith(classOf[JUnitRunner])
class AggregateRootSpec extends Spec with ShouldMatchers {

  describe("flushing pending events") {
    it("should reset pending events") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List()))
        .acceptSomeCommand
        .flushPendingEvents { (r, events) => Some(nextCommitRevision(r))}
        .pendingEvents should be === Nil
    }

    it("should update commitRevision when no previous commitRevision") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List()))
        .acceptSomeCommand
        .flushPendingEvents { (r, events) => Some(nextCommitRevision(r)) }
        .snapshot.includesCommitsUpToRevision should be === 1
    }

    it("should update commitRevision when previous commitRevision") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List()))
        .acceptSomeCommand
        .flushPendingEvents { (r, events) => Some(nextCommitRevision(r)) }
        .snapshot.includesCommitsUpToRevision should be === 2
    }
  }

  describe("snapshotting") {
    it("should be disabled when there are pending events") {
      val aggregateRoot = SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List()))
        .acceptSomeCommand
      evaluating { aggregateRoot.snapshot } should produce[AssertionError]
    }
  }

  describe("Commit Revision") {
    it("should be incremented when snapshots and events are used to create the aggregate root") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List(event2)))
        .commitRevision should be === Some(2)
    }

    it("should be none when no snapshots nor any event is used to create the aggregate root") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List()))
        .commitRevision should be === None
    }

    it("should be incremented when a snapshot is used to create the aggregate root") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List()))
        .commitRevision should be === Some(1)
    }

    it("should be incremented when an event is used to create the aggregate root") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(None, List(event1)))
        .commitRevision should be === Some(1)
    }

    it("should not take pending events in consideration") {
      SomeAggregateRoot.loadFromHistory("myId", eventHistory(Some(snapshotAfterEvent1), List(event2)))
        .acceptSomeCommand
        .commitRevision should be === Some(2)
    }
  }

  def event1 = MyEvent("1-data")
  def event2 = MyEvent("2-data")

  def snapshotAfterEvent1 = StreamSnapshot(state=SomeAggregateRootState(event1Applied = true),includesCommitsUpToRevision=1)
}