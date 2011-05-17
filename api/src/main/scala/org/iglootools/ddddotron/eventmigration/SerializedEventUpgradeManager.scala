package org.iglootools.ddddotron.eventmigration;
trait SerializedEventUpgradeManager {
  def upgradeToMostRecentVersion(serializedEvent: SerializedEvent): SerializedEvent
}