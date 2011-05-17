package org.iglootools.ddddotron

import org.iglootools.commons.joda.DateTimes
import scala.Option._

package object core {
  // Core
  type GUID = String
  type Revision = Long
  // helpers
  def currentUtcDateTime = DateTimes.currentUtcDateTime
  def commitRevisionOne = 1L
  def nextCommitRevision(revision: Option[Revision]) = revision map (_ + 1) getOrElse (commitRevisionOne)

}