package org.iglootools.ddddotron.storage

import org.joda.time.DateTime
import org.iglootools.ddddotron.core._

sealed trait EventSpecification { }

final case class EventTimestampSpecification(from: DateTime) extends EventSpecification {
  require(from != null, "from is required")
}

final case class RevisionSpecification(from: Revision) extends EventSpecification {

}

final case class StreamIdSpecification(streamType: String, streamId: GUID) extends EventSpecification {
  require(streamType != null, "streamType is required")
  require(streamId != null, "streamId is required")
}

final case class StreamTypeSpecification(streamType: String) extends EventSpecification {
  require(streamType != null, "streamType is required")
}

final case class Filter(val specifications: List[EventSpecification]) {
  if(specifications.exists(_.getClass == classOf[RevisionSpecification])) {
    assume(specifications.exists(_.getClass == classOf[StreamIdSpecification]), "RevisionSpecification cannot be used without StreamIdSpecification")
  }

  def containsRestrictions = specifications.size > 0
}