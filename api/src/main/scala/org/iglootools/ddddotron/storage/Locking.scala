package org.iglootools.ddddotron.storage

import org.iglootools.ddddotron.core._

object Locking {
  def expectedRevisions(revision: Option[Revision], numberOfEvents: Int): Seq[Long] = {
    require(numberOfEvents >= 0, "numberOfEvents must be positive")
    val revisionOffsets = 1 to numberOfEvents
    revisionOffsets map (revision.getOrElse(commitRevisionOne-1) + _)
  }
}