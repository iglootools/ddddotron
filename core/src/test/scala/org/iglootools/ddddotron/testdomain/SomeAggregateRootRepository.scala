package org.iglootools.ddddotron.testdomain

import org.iglootools.ddddotron._

object SomeAggregateRootRepository {
  def apply[E <: Event, S<:AnyRef, AR <: AggregateRoot[E,S,AR]]()(implicit aggregateRootFactory: AggregateRootFactory[E,S,AR],
                      eventStore: EventStore,
                      eventBus: EventBus,
                      mfs: Manifest[S],
                      mfe: Manifest[E]) = {
    new Repository("SomeAggregateRoot")
  }
}