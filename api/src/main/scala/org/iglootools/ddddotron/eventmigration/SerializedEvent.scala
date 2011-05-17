package org.iglootools.ddddotron.eventmigration;

import org.iglootools.ddddotron.core.commitRevisionOne

final case class SerializedEvent(eventType: String, eventVersion: Int, data: String) {
  require(Option(eventType) exists (_.nonEmpty), "eventType is required")
  require(data != null, "data is required")
  require(eventVersion >= commitRevisionOne, "Version %s is not accepted. Minimum version is %s".format(eventVersion, commitRevisionOne))
}