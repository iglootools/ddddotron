package org.iglootools.ddddotron.tests

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.infrastructure._
import org.iglootools.ddddotron.testdomain._
import org.iglootools.commons.scalatest.SpringSupport
import org.junit.runner.RunWith

import javax.sql.DataSource
import org.scalatest.{BeforeAndAfterEach, Spec}
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.{TransactionStatus, TransactionDefinition, PlatformTransactionManager}
import org.iglootools.ddddotron.storage.Locking


@RunWith(classOf[JUnitRunner])
class RepositorySpec extends Spec with ShouldMatchers with SpringSupport with BeforeAndAfterEach {
  val locations = Array("classpath:/org/iglootools/ddddotron/infrastructure/eventstore/ddddotron-eventstore-memory-context.xml")

  implicit val dataSource = getBean[DataSource]("hsqldbDataSource")
  implicit val eventSerializer = new JsonEventSerializer(List(Serializable[MyEvent], Serializable[SomeCommandAccepted], Serializable[AnotherCommandAccepted], Serializable[SomethingCreated]))
  implicit val aggregateRootStateSerializer: AggregateRootStateSerializer = new JsonAggregateRootStateSerializer
  implicit val serializedEventUpgradeManager = new DefaultSerializedEventUpgradeManager(List())
  implicit val eventStore: EventStore = new JdbcEventStore
  implicit val eventBus: EventBus = new NullEventBus
  implicit val someAggregateRootFactory = SomeAggregateRoot
  val repository = SomeAggregateRootRepository()
  val txManager = getBean(classOf[PlatformTransactionManager])
  var tx: Option[TransactionStatus] = None

  override def beforeEach() {
    tx = Some(txManager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS)))
    new SimpleJdbcTemplate(dataSource).update("""DELETE FROM event""")
    eventStore.deleteAllSnapshots
  }

  override def afterEach() {
    tx foreach (txManager.commit(_))
  }

  describe("Event Store") {
    it("should commit pending events") {
      val commit = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
      eventStore.attemptCommit(commit)
      eventStore.committedEvents("MyAggregate", "myId") should be === toCommittedEvents(commit)
    }

    it("should raise concurrency exception if two commits have same version") {
      val commit1 = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
      val commit2 = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("3-data")))

      eventStore.attemptCommit(commit1)
      evaluating { eventStore.attemptCommit(commit2) } should produce [ConcurrencyException]
    }
  }
  describe("Event Dispatcher Storage Support") {
    it("should retrieve undispatched events") {
      val commit1Aggregate1 = CommitAttempt("MyAggregate", "myId1", None, List(MyEvent("11-data"), MyEvent("12-data")))
      val commit2Aggregate1 = CommitAttempt("MyAggregate", "myId1", commit1Aggregate1.expectedEventRevisions.lastOption, List(MyEvent("13-data"), MyEvent("14-data")))

      val commit1Aggregate2 = CommitAttempt("MyAggregate", "myId2", None, List(MyEvent("21-data"), MyEvent("22-data")))
      val commit2Aggregate2 = CommitAttempt("MyAggregate", "myId2", commit1Aggregate2.expectedEventRevisions.lastOption, List(MyEvent("23-data"), MyEvent("24-data")))

      val allCommits = List(commit1Aggregate1, commit1Aggregate2, commit2Aggregate1, commit2Aggregate2)
      allCommits foreach (eventStore.attemptCommit(_))

      eventStore.undispatchedCommittedEvents.size should be === allCommits.flatMap(_.events).size

      // now check specific order, as it is _really_ important
      eventStore.undispatchedCommittedEvents.filter (_.streamId == "myId1") should be ===  List(commit1Aggregate1, commit2Aggregate1).flatMap(toCommittedEvents(_))
      eventStore.undispatchedCommittedEvents.filter (_.streamId == "myId2") should be ===  List(commit1Aggregate2, commit2Aggregate2).flatMap(toCommittedEvents(_))

    }

    it("should mark undispatched events as marked") {
      val commit = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
      eventStore.attemptCommit(commit)
      eventStore.markAsDispatched(eventStore.undispatchedCommittedEvents()(0))
      eventStore.undispatchedCommittedEvents.size should be === toCommittedEvents(commit).size -1
    }

    it("should not leave any undispatched event") {
      val commit = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
      eventStore.attemptCommit(commit)
      eventStore.doWithUndispatchedCommittedEvents (eventStore.markAsDispatched(_))
      eventStore.undispatchedCommittedEvents should be ('empty)
    }

    it("should indicate whether an event is dispatched") {
      val commit = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
      eventStore.attemptCommit(commit)
      val List(dispatched, undispatched) = eventStore.undispatchedCommittedEvents()
      eventStore.markAsDispatched(dispatched)
      eventStore.isDispatched(dispatched) should be === true
      eventStore.isDispatched(undispatched) should be === false
    }

  }



  describe("Snapshot Store") {
    it("should not retrieve any aggregate root snapshot when none has been saved") {
      eventStore.streamSnapshotOption("MyAggregate", "MyAggregateId") should be === None

    }

    it("should save and retrieve aggregate root snapshot") {
      val snapshot = StreamSnapshot(state=SomeAggregateRootState(event1Applied = true, event2Applied = false), includesCommitsUpToRevision=5)
      eventStore.saveStreamSnapshot("MyAggregate", "MyAggregateId", snapshot)
      eventStore.streamSnapshotOption[SomeAggregateRootState]("MyAggregate", "MyAggregateId") should be === Some(snapshot)
    }

    it("should update aggregate root snapshot") {
      val snapshot = StreamSnapshot(state=SomeAggregateRootState(event1Applied = true, event2Applied = true), includesCommitsUpToRevision=6)
      eventStore.saveStreamSnapshot("MyAggregate", "MyAggregateId", snapshot)
      eventStore.streamSnapshotOption[SomeAggregateRootState]("MyAggregate", "MyAggregateId") should be === Some(snapshot)
    }

    it("should delete all snapshots") {
      val someSnapshot = StreamSnapshot(state=SomeAggregateRootState(), includesCommitsUpToRevision=2)
      eventStore.saveStreamSnapshot("MyAggregate", "MyAggregateId", someSnapshot)
      eventStore.saveStreamSnapshot("MyAggregate", "MyAggregateId2", someSnapshot)
      eventStore.saveStreamSnapshot("MyOtherAggregate", "MyAggregateId2", someSnapshot)
      eventStore.deleteAllSnapshots
      eventStore.streamSnapshotOption("MyAggregate", "MyAggregateId") should be === None
      eventStore.streamSnapshotOption("MyAggregate", "MyAggregateId2") should be === None
      eventStore.streamSnapshotOption("MyOtherAggregate", "MyAggregateId") should be === None
    }

    it("should delete snapshots of given type") {
      val someSnapshot = StreamSnapshot(state=SomeAggregateRootState(), includesCommitsUpToRevision=2)
      eventStore.saveStreamSnapshot("MyAggregate", "MyAggregateId", someSnapshot)
      eventStore.saveStreamSnapshot("AnotherAggregate", "anything", someSnapshot)
      eventStore.saveStreamSnapshot("MyOtherAggregate", "whatever", someSnapshot)
      eventStore.deleteSnapshotsOfTypes(List("AnotherAggregate", "MyOtherAggregate"))
      eventStore.streamSnapshotOption[SomeAggregateRootState]("MyAggregate", "MyAggregateId") should not be None
      eventStore.streamSnapshotOption("AnotherAggregate", "anything") should be === None
      eventStore.streamSnapshotOption("MyOtherAggregate", "whatever") should be === None
    }
  }

  describe("Repository") {
    it("should let us flush events") {
      val notFlushed = SomeAggregateRoot("myId")
        .acceptSomeCommand
        .acceptAnotherCommand
      val flushed = repository.commitPendingEvents(notFlushed)

      val reconstructed = repository.getById("myId")

      flushed.snapshot should not be None
      reconstructed.snapshot should be === flushed.snapshot
    }

    it("should let us fush several times and give the same effect") {
      val notFlushed = SomeAggregateRoot("myId")
        .acceptSomeCommand
      val afterFlush1 = repository.commitPendingEvents(notFlushed)
      val afterAnotherCommand = afterFlush1.acceptAnotherCommand
      val afterFlush2 = repository.commitPendingEvents(afterAnotherCommand)

      val reconstructed = repository.getById("myId")
      reconstructed.snapshot should be === afterFlush2.snapshot
    }

    it("should throw concurrency exception if several instances of the same aggregate root produce events") {
      val root1 = SomeAggregateRoot("myId")
        .acceptSomeCommand

      repository.commitPendingEvents(root1)

      val root21 = repository.getById("myId")
        .acceptAnotherCommand

      val root22 = repository.getById("myId")
        .acceptAnotherCommand

      repository.commitPendingEvents(root21)

      evaluating { repository.commitPendingEvents(root22) } should produce[ConcurrencyException]
    }

    it("should persist and make use of snapshots") {
      val root1 = SomeAggregateRoot("myId")
        .acceptSomeCommand
      val flushed = repository.commitPendingEvents(root1)
      repository.persistSnapshot(flushed)

      val reconstructed = repository.getById("myId")
      reconstructed.snapshot should be === flushed.snapshot

    }
  }

  def toCommittedEvents[E <: Event](commit: CommitAttempt[E]) = {
    commit.events zip commit.expectedEventRevisions map {case (e,r) => CommittedEvent(commit.streamType, commit.streamId, r, e)}
  }
}