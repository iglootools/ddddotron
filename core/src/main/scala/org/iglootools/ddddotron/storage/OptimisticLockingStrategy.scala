package org.iglootools.ddddotron.storage
import org.iglootools.ddddotron.core._

/**
 * We expect the underlying storage to throw an exception if the given revision is not the latest one
 * None should be used for the first event of a given Aggrgate
 */
protected[ddddotron] final case class OptimisticLockingStrategy(revision: Option[Int]) extends LockingStrategy {
  revision.foreach { r =>
    require(r >= commitRevisionOne-1, "the first revision is 1")
  }
}