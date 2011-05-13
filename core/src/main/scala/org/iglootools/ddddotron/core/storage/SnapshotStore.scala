package org.iglootools.ddddotron.core.storage

import org.iglootools.ddddotron.core._

trait SnapshotStore {
  def streamSnapshotOption[S <: AnyRef](streamType: String, streamId: GUID)(implicit mf: Manifest[S]): Option[StreamSnapshot[S]]
  def saveStreamSnapshot[S <: AnyRef](streamType: String, streamId: GUID, streamSnapshot: StreamSnapshot[S])(implicit mf: Manifest[S])
  def deleteAllSnapshots(): Unit
  def deleteSnapshotsOfTypes(types: List[String]): Unit
}