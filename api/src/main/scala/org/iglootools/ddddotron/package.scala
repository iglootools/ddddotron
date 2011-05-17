package org.iglootools

package object ddddotron {
  // Core
  type GUID = core.GUID
  type Revision = core.Revision
  type AggregateRoot[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]] = core.AggregateRoot[E, S, AR]
  type Flushable[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]] = core.Flushable[E,S,AR]
  type Snapshottable[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]] = core.Snapshottable[E,S,AR]
  type Copyable[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]] = core.Copyable[E,S,AR]
  type EventSourced[E <: Event, ES <: EventSourced[E,ES]] = core.EventSourced[E,ES]
  type AggregateRootIdentityProvider = core.AggregateRootIdentityProvider
  type AggregateRootFactory[E <: Event, S <: AnyRef, AR <: AggregateRoot[E, S, AR]] = core.AggregateRootFactory[E,S,AR]

  type CommittedEvent[+E <: Event] = core.CommittedEvent[E]
  val CommittedEvent = core.CommittedEvent
  type Command = core.Command
  type Event = core.Event
  type EventHistory[+E <: Event, +S <: AnyRef] = core.EventHistory[E,S]
  val EventHistory = core.EventHistory
  type StreamSnapshot[S <: AnyRef] = core.StreamSnapshot[S]
  val StreamSnapshot = core.StreamSnapshot

  // Storage
  type CommitAttempt[+E <: Event] = storage.CommitAttempt[E]
  val CommitAttempt = storage.CommitAttempt
  type Repository[E <: Event, S<:AnyRef, AR <: AggregateRoot[E,S, AR]] = storage.Repository[E,S,AR]
  type EventStore = storage.EventStore
  type ConcurrencyException = storage.ConcurrencyException
  val OptimisticLockingStrategy = storage.OptimisticLockingStrategy
  type OptimisticLockingStrategy = storage.OptimisticLockingStrategy

  // Enterprise Integration
  type EventBus = ei.EventBus

   // Serialization (used by infrastructure)
  type EventSerializer = serialization.EventSerializer
  type AggregateRootStateSerializer = serialization.StreamStateSerializer
  type Serializable[T <: Event] = serialization.Serializable[T]
  val Serializable = serialization.Serializable

  // Event Migration (used by infrastructure)
  type DefaultSerializedEventUpgradeManager  = eventmigration.DefaultSerializedEventUpgradeManager
  type SerializedEvent = eventmigration.SerializedEvent
  val SerializedEvent = eventmigration.SerializedEvent
  type SerializedEventUpgradeManager = eventmigration.SerializedEventUpgradeManager
  type SerializedEventUpgrader = eventmigration.SerializedEventUpgrader

  // infrastructure
//  type InMemoryEventStore = infrastructure.eventstore.InMemoryEventStore
//  type JsonAggregateRootStateSerializer = infrastructure.serialization.JsonAggregateRootStateSerializer
//  type JsonEventSerializer = infrastructure.serialization.JsonEventSerializer
//  type NullEventBus = infrastructure.ei.NullEventBus


  // helpers
  def currentUtcDateTime = core.currentUtcDateTime
  def commitRevisionOne = core.commitRevisionOne
  def nextCommitRevision(revision: Option[Revision]) = core.nextCommitRevision(revision)


}