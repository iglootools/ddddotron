package org.iglootools.ddddotron.core.storage

import org.iglootools.ddddotron.core._

trait EventDispatcherStorageSupport {
  def markAsDispatched[E <: Event](committedEvent: CommittedEvent[E]): Unit

  /**
   * Will call <code>f</code> in ascending revision order
   */
  def doWithUndispatchedCommittedEvents(f: CommittedEvent[Event] => Unit) : Unit

  /**
   * Do not use this in production code unless you are sure you have a loadful of RAM
   * <p>
   *   Events will be ordered by commit/event revision
   * </p>
   */
  def undispatchedCommittedEvents(): List[CommittedEvent[Event]] = {
    val events = scala.collection.mutable.ListBuffer[CommittedEvent[Event]]()
    doWithUndispatchedCommittedEvents { case committedEvent =>
      events += committedEvent
    }
    events.toList
  }

  def isDispatched[E<:Event](committedEvent: CommittedEvent[E]): Boolean
}