package org.iglootools.ddddotron.infrastructure.eventstore

import org.iglootools.ddddotron.storage._
import org.iglootools.ddddotron.ei._
import org.iglootools.ddddotron.serialization._
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import scala.collection.JavaConversions._
import scalaj.collection.Imports._
import org.springframework.dao.DataIntegrityViolationException
import org.iglootools.ddddotron.eventmigration.{SerializedEvent, SerializedEventUpgradeManager}
import org.iglootools.ddddotron.core._
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.joda.time.{DateTime, DateTimeZone}
import com.google.common.io.CharStreams
import java.sql.{Statement, PreparedStatement, Connection, ResultSet}
import grizzled.slf4j.Logging
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate}
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}
import org.springframework.jdbc.core._
import javax.management.remote.rmi._RMIConnection_Stub

/**
 * FIXME: there are several non-in memory specific responsibilities that should be extracted and properly unit-tested :
 * - Event Serialization / Deserialization
 * - Serialized Event Migration
 * - Aggregate root Serialization / Deserialization
 *
 * @see http://blog.jonathanoliver.com/2009/03/event-sourcing-persistence.html
 * @see https://github.com/gregoryyoung/m-r/blob/master/SimpleCQRS/EventStore.cs
 * @see http://jasondentler.com/blog/2010/10/simple-cqrs-nhibernate-event-store/
 */
class JdbcEventStore(implicit eventSerializer: EventSerializer,
                     streamStateSerializer: StreamStateSerializer,
                     serializedEventUpgradeManager: SerializedEventUpgradeManager,
                     dataSource: DataSource,
                     txManager: PlatformTransactionManager) extends EventStore with Logging {
  val jdbcTemplate = new SimpleJdbcTemplate(dataSource)
  val tx = executeInTransaction[Unit](txManager)_
  /**
   * @throws ConcurrencyException
   */
  def attemptCommit[E <: Event](attempt: CommitAttempt[E]) = {
    try {
      debug("Attempting to commit: %s.".format(attempt))

      tx {
        for ((e, r) <-  attempt.events zip attempt.expectedEventRevisions) {
          debug("[%s] Inserting Event revision: %s".format(r, r))
          persistEvent(attempt.streamType, attempt.streamId, r, e)
        }
      }


    } catch {
      case e: DataIntegrityViolationException =>
        throw new ConcurrencyException("Trying to insert commit %s using a current revision of: %s. Duplicate version".format(
          attempt, attempt.revision), e)
      case t: Throwable => throw t
    }
  }

  protected[this] def persistEvent[E <: Event](streamType: String, streamId: GUID, revision: Revision, event: E) = {
    require(event.timestamp.getZone == DateTimeZone.UTC, "Event Date must be in the UTC Zone")

    jdbcTemplate.update("""INSERT INTO event(stream_type, stream_id, revision, payload_type, payload_version, payload, event_timestamp)
        VALUES(?, ?, ?, ?, ?, ?, ?)""",
      streamType,
      streamId,
      revision.asInstanceOf[AnyRef],
      event.eventType,
      event.version.asInstanceOf[AnyRef],
      eventSerializer.serialize(event),
      event.timestamp.toDate)
  }

  def doWithCommittedEvents(streamType: String, streamId: GUID, fromRevision: Revision = commitRevisionOne)(f: CommittedEvent[Event] => Unit) {
    new JdbcTemplate(dataSource).query(
      CommittedEventRowMapper.QuerySelectFrom + """WHERE stream_type=?
        AND stream_id=?
        AND revision >= ?
        ORDER BY revision""",
      Array(streamType, streamId, fromRevision.asInstanceOf[AnyRef]),
      rowCallbackHandlerDelegatingToF(f))
  }

  def doWithCommittedEvents(filter: Filter)(f: CommittedEvent[Event] => Unit) {
    new JdbcTemplate(dataSource).query(CommittedEventRowMapper.QuerySelectFrom +
      whereClauses(filter.specifications).mkString(if(filter.containsRestrictions) " WHERE " else "", " AND ", " ") +
      "ORDER BY id",
      parameters(filter.specifications).toArray,
      rowCallbackHandlerDelegatingToF(f)
    )
  }

  private[this] def whereClauses(filter: List[EventSpecification]): List[String] = {
    filter map {
      case s: EventTimestampSpecification => "event_timestamp>=?"
      case s: RevisionSpecification => "revision >= ?"
      case s: StreamIdSpecification => "stream_type=? AND stream_id=?"
      case s: StreamTypeSpecification => "stream_type=?"
    }
  }

  private[this] def parameters(filter: List[EventSpecification]): List[AnyRef] = {
    filter flatMap {
      case s: EventTimestampSpecification => List(s.from.toDate)
      case s: RevisionSpecification => List(s.from.asInstanceOf[AnyRef])
      case s: StreamIdSpecification => List(s.streamType, s.streamId)
      case s: StreamTypeSpecification => List(s.streamType)
    }
  }

  def markAsDispatched[E <: Event](committedEvent: CommittedEvent[E]) {
    import committedEvent._
    val rc = jdbcTemplate.update("""UPDATE event
      SET dispatched=true
      WHERE stream_type = ?
      AND stream_id = ?
      AND revision = ?
      """, streamType, streamId, commitRevision.asInstanceOf[AnyRef])

    debug("Number of rows marked as disptached: %s".format(rc))
  }

  def isDispatched[E<:Event](committedEvent: CommittedEvent[E]): Boolean = {
    import committedEvent._
    jdbcTemplate.queryForObject("""SELECT dispatched
        FROM event
        WHERE stream_type = ?
        AND stream_id = ?
        AND revision = ?""", classOf[java.lang.Boolean],
      streamType, streamId, commitRevision.asInstanceOf[AnyRef]) == true

  }

  def doWithUndispatchedCommittedEvents(f: CommittedEvent[Event] => Unit) {
    new JdbcTemplate(dataSource).query(
      CommittedEventRowMapper.QuerySelectFrom + """WHERE dispatched = false ORDER BY stream_type, stream_id, revision""",
      rowCallbackHandlerDelegatingToF(f))
  }

  def streamSnapshotOption[T <: AnyRef](streamType: String, streamId: GUID)(implicit mf: Manifest[T]): Option[StreamSnapshot[T]] = {
    val rowMapper = new RowMapper[StreamSnapshot[T]]() {
      def mapRow(rs: ResultSet, rowNum: Int): StreamSnapshot[T] = {

        val includesCommitsUpToRevision = rs.getInt("includes_commits_up_to_revision").asInstanceOf[Int]
        val snapshotData = rs.getString("snapshot_payload")

        new StreamSnapshot[T](streamStateSerializer.deserialize[T](snapshotData), includesCommitsUpToRevision)
      }
    };

    jdbcTemplate.query("""SELECT stream_type, stream_id, includes_commits_up_to_revision, snapshot_payload FROM stream_snapshot
       WHERE stream_type=?
       AND stream_id=?""", rowMapper, streamType, streamId).asScala.headOption
  }

  def saveStreamSnapshot[T <: AnyRef](streamType: String, streamId: GUID, streamSnapshot: StreamSnapshot[T])(implicit mf: Manifest[T]) = {
    val serializedSnapshotData = streamStateSerializer.serialize(streamSnapshot.state)

    val rowsUpdated = jdbcTemplate.update("""UPDATE stream_snapshot
      SET includes_commits_up_to_revision=?, snapshot_payload=?
      WHERE stream_type=?
      AND stream_id=?""", streamSnapshot.includesCommitsUpToRevision.asInstanceOf[AnyRef], serializedSnapshotData, streamType, streamId)
    if(rowsUpdated == 0) {
      jdbcTemplate.update(""" INSERT INTO stream_snapshot(stream_type, stream_id, includes_commits_up_to_revision, snapshot_payload)
      VALUES(?, ?, ?, ?)""", streamType, streamId, streamSnapshot.includesCommitsUpToRevision.asInstanceOf[AnyRef], serializedSnapshotData)
    }

  }



  def deleteSnapshotsOfTypes(types: List[String]) {
    jdbcTemplate.update("DELETE from stream_snapshot WHERE stream_type IN (:types)", Map("types" -> types.asJava).asJava)
  }

  def deleteAllSnapshots() {
    jdbcTemplate.update("DELETE from stream_snapshot")
  }

  private[this] def executeInTransaction[T](txManager: PlatformTransactionManager)(f: => T): T = {
    new TransactionTemplate(txManager).execute(new TransactionCallback[T]() {
      def doInTransaction(p1: TransactionStatus): T = {
        f
      }
    })
  }


  private[this] def rowCallbackHandlerDelegatingToF(f: CommittedEvent[Event] => Unit): RowCallbackHandler = {
    new RowCallbackHandler() {
      val rowMapper = new CommittedEventRowMapper(eventSerializer, serializedEventUpgradeManager)
      var rowNum: Int = 0

      def processRow(resultSet: ResultSet) {
        val e = rowMapper.mapRow(resultSet, rowNum)
        f(e)
        debug("Mapped Row %s to %s".format(rowNum, e))
        rowNum += 1
      }
    }
  }
}

object CommittedEventRowMapper {
  val QuerySelectFrom = """SELECT stream_type, stream_id, revision, payload_type, payload_version, payload, event_timestamp FROM event """
}

class CommittedEventRowMapper(eventSerializer: EventSerializer, serializedEventUpgradeManager:SerializedEventUpgradeManager) extends RowMapper[CommittedEvent[Event]] {
  def mapRow(rs: ResultSet, rowNum: Int): CommittedEvent[Event] = {
    val streamType = rs.getString("stream_type")
    val streamId = rs.getString("stream_id")
    val payload = rs.getString("payload")
    val payloadType = rs.getString("payload_type")
    val payloadVersion = rs.getInt("payload_version").asInstanceOf[Int]
    val serializedEvent = serializedEventUpgradeManager.upgradeToMostRecentVersion(SerializedEvent(payloadType, payloadVersion, payload))
    val revision = rs.getLong("revision").asInstanceOf[Int]
    val event = eventSerializer.deserialize(serializedEvent.eventType, serializedEvent.data)

    CommittedEvent(streamType, streamId, revision, event)

  }
}
