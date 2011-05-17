package org.iglootools.ddddotron.eventmigration

trait SerializedEventUpgrader {
  def eventType: String
  def fromVersion: Int
  def toVersion: Int = fromVersion + 1
  def convert(serializedEvent: SerializedEvent): SerializedEvent = serializedEvent.copy(data = doConvertData(serializedEvent.data), eventVersion=toVersion)

  protected[this] def doConvertData(data: String): String
}