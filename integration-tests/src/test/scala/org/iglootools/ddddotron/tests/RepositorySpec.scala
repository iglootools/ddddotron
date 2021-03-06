package org.iglootools.ddddotron.tests

import org.scalatest.junit.JUnitRunner
import org.iglootools.commons.scala._
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.infrastructure._
import org.iglootools.ddddotron.testdomain._
import org.iglootools.commons.scalatest.SpringSupport
import org.junit.runner.RunWith

import javax.sql.DataSource
import org.scalatest.{BeforeAndAfterEach, Spec}
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.transaction.{TransactionStatus, TransactionDefinition, PlatformTransactionManager}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate, DefaultTransactionDefinition}
import storage.{Locking}
import javax.management.remote.rmi._RMIConnection_Stub
import org.scala_tools.time.Imports._
import org.joda.time.DateTime

@RunWith(classOf[JUnitRunner])
abstract class RepositorySpec extends Spec with SpringSupport with TableDrivenPropertyChecks with ShouldMatchers with BeforeAndAfterEach {
  def databaseName: String

  var currentTx: Option[TransactionStatus] = None

  implicit val txManager = getBean(classOf[PlatformTransactionManager])
  implicit val dataSource = getBean(classOf[DataSource])

  implicit val eventSerializer = new JsonEventSerializer(List(Serializable[MyEvent], Serializable[SomeCommandAccepted], Serializable[AnotherCommandAccepted], Serializable[SomethingCreated]))
  implicit val aggregateRootStateSerializer: AggregateRootStateSerializer = new JsonAggregateRootStateSerializer
  implicit val serializedEventUpgradeManager = new DefaultSerializedEventUpgradeManager(List())
  implicit val eventBus: EventBus = new NullEventBus
  implicit val eventStore: EventStore = new JdbcEventStore

  implicit val someAggregateRootFactory = SomeAggregateRoot
  val repository = SomeAggregateRootRepository()

  describe(databaseName) {

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

      it("should support non-ASCII characters in event data") {
        val commit = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("some characters like that: éèàçç")))
        eventStore.attemptCommit(commit)
        eventStore.committedEvents("MyAggregate", "myId") should be === toCommittedEvents(commit)
      }

      it("should return committedEvents matching stream id filter") {
        val commit1 = CommitAttempt("MyAggregate", "myId", None, List(MyEvent("1-data"), MyEvent("2-data")))
        val commit2 = CommitAttempt("MyAggregate", "myId2", None, List(MyEvent("1-data"), MyEvent("2-data")))
        List(commit1, commit2) foreach (eventStore.attemptCommit(_))
        eventStore.committedEvents(Filter(List(StreamIdSpecification("MyAggregate", "myId")))) should be === toCommittedEvents(commit1)
      }

       it("should return committedEvents matching stream type filter") {
        val commit1 = CommitAttempt("MyAggregate", "someId", None, List(MyEvent("1-data"), MyEvent("2-data")))
        val commit2 = CommitAttempt("AnotherAggregateType", "someId", None, List(MyEvent("1-data"), MyEvent("2-data")))
        List(commit1, commit2) foreach (eventStore.attemptCommit(_))
        eventStore.committedEvents(Filter(List(StreamTypeSpecification("MyAggregate")))) should be === toCommittedEvents(commit1)
      }

      it("should return committedEvents matching revision filter") {
        val commit1 = CommitAttempt("MyAggregate", "someId", None, List(MyEvent("1-data"), MyEvent("2-data")))
        val commit2 = CommitAttempt("MyAggregate", "someId", commit1.expectedEventRevisions.lastOption, List(MyEvent("3-data"), MyEvent("4-data")))
        val commit3 = CommitAttempt("MyAggregate", "someId", commit2.expectedEventRevisions.lastOption, List(MyEvent("5-data"), MyEvent("6-data")))
        List(commit1, commit2, commit3) foreach (eventStore.attemptCommit(_))
        eventStore.committedEvents(
          Filter(
            List(
              StreamIdSpecification("MyAggregate", "someId"),
              RevisionSpecification(commit2.expectedEventRevisions.head)))) should be === (List(commit2, commit3) flatMap (toCommittedEvents(_)))
      }

      it("should return events committed after timestamp") {
        val anHourAgo = currentUtcDateTime - 1.hour

        val commit1 = CommitAttempt("MyAggregate", "someId", None, List(MyEvent("1-data", timestamp = anHourAgo), MyEvent("2-data", timestamp = anHourAgo)))
        val commit2 = CommitAttempt("MyAggregate", "someId", commit1.expectedEventRevisions.lastOption, List(MyEvent("3-data"), MyEvent("4-data")))
        val commit3 = CommitAttempt("MyAggregate", "someId", commit2.expectedEventRevisions.lastOption, List(MyEvent("5-data"), MyEvent("6-data")))
        List(commit1, commit2, commit3) foreach (eventStore.attemptCommit(_))
        eventStore.committedEvents(
          Filter(
            List(
              StreamIdSpecification("MyAggregate", "someId"),
              EventTimestampSpecification(currentUtcDateTime - 1.minute)))) should be === (List(commit2, commit3) flatMap (toCommittedEvents(_)))
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
  }

  override def beforeEach() {
    //    currentTx = Some(beginTransaction(currentTxManager.get))
    new SimpleJdbcTemplate(dataSource).update("""DELETE FROM event""")
    eventStore.deleteAllSnapshots
    //    commitTransaction(currentTxManager.get, currentTx.get)

    //    currentTx = Some(beginTransaction(currentTxManager.get))
  }

  override def afterEach() {
    //    commitTransaction(currentTxManager.get, currentTx.get)
  }

  def beginTransaction(txManager:PlatformTransactionManager): TransactionStatus = {
    txManager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
  }

  def commitTransaction(txManager:PlatformTransactionManager, tx: TransactionStatus) {
    txManager.commit(tx)
  }

  def toCommittedEvents[E <: Event](commit: CommitAttempt[E]) = {
    commit.events zip commit.expectedEventRevisions map {case (e,r) => CommittedEvent(commit.streamType, commit.streamId, r, e)}
  }
}