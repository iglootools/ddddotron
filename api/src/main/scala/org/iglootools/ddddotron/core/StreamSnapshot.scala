package org.iglootools.ddddotron.core

protected[ddddotron] final case class StreamSnapshot[+S](val state: S, val includesCommitsUpToRevision: Revision) {
  require(state != null, "state is required")
  require(includesCommitsUpToRevision >= commitRevisionOne , "commit revision must be at least: %s".format(commitRevisionOne))
}