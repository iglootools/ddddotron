package org.iglootools.ddddotron

package object infrastructure {
  // EventStore
  type JdbcEventStore = eventstore.JdbcEventStore

  // Serialization
  type JsonAggregateRootStateSerializer = serialization.JsonAggregateRootStateSerializer
  type JsonEventSerializer = serialization.JsonEventSerializer

  // Enterprise Integration
  type NullEventBus = ei.NullEventBus
}