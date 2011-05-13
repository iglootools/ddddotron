package org.iglootools.ddddotron.eventmigration

final class DefaultSerializedEventUpgradeManager(eventUpgraders: List[SerializedEventUpgrader]) extends SerializedEventUpgradeManager {
  require(eventUpgraders != null, "eventUpgraders is required")

  val upgraders: Map[String, List[SerializedEventUpgrader]] = eventUpgraders.groupBy(_.eventType)

  def upgradeToMostRecentVersion(serializedEvent: SerializedEvent): SerializedEvent = {
    val upgradersApplyingToEvent = upgraders.get(serializedEvent.eventType).getOrElse(List())
      .filter(_.fromVersion >= serializedEvent.eventVersion)
      .sortBy (_.fromVersion)

    var currentSerializedEvent = serializedEvent
    upgradersApplyingToEvent foreach { u =>
      currentSerializedEvent = new SafeEventUpgrader(u).convert(currentSerializedEvent)
    }
    currentSerializedEvent
  }

  class SafeEventUpgrader(wrapped: SerializedEventUpgrader) extends SerializedEventUpgrader {
    override def convert(serializedEvent: SerializedEvent): SerializedEvent = {
      assume(wrapped.fromVersion == serializedEvent.eventVersion,
        "Upgrader converts from version %s but event's version is %s. SerializedEvent before conversions: %s".format(
          wrapped.fromVersion,
          serializedEvent.eventVersion,
          serializedEvent))
      wrapped.convert(serializedEvent)
    }

    def fromVersion: Int = wrapped.fromVersion
    def eventType: String = wrapped.eventType
    override def toVersion: Int = wrapped.toVersion

    protected[this] def doConvertData(data: String): String = throw new RuntimeException() // do nothing
  }

}