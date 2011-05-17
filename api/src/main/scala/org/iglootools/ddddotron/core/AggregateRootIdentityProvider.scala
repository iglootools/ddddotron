package org.iglootools.ddddotron.core

protected[ddddotron] trait AggregateRootIdentityProvider {
  def aggregateRootType: String
  def aggregateRootId: GUID
}