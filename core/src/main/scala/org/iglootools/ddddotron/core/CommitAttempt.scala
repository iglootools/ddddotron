package org.iglootools.ddddotron.core

import org.joda.time.{DateTimeZone, DateTime}
import storage.Locking

/**
 * @param revision is the revision on which this commit is based
 */
final case class CommitAttempt[+E <: Event](val streamType: String,
                                     val streamId: String,
                                     val revision: Option[Revision],
                                     val events: List[E]) {
  require(Option(streamType) exists (_.nonEmpty), "streamType is required")
  require(Option(streamId) exists (_.nonEmpty), "streamId is required")
  require(revision != null, "revision is required")
  require(Option(events) exists (_.nonEmpty), "events is required")

  def expectedEventRevisions = Locking.expectedRevisions(revision, events.size)
}