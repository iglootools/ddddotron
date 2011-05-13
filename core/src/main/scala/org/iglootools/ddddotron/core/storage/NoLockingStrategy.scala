package org.iglootools.ddddotron.core.storage

/**
 * This strategy is to be used when a collision is expected to be merged later.
 * It is similar to DVCS that keep a tree
 */
protected[ddddotron] final case class NoLockingStrategy(parentVersion: Int) extends LockingStrategy {
  require(parentVersion >= 0, "the first version is 1. Having a parent of 0 means there is no parent (first event of a given stream)")
}


