package org.iglootools.ddddotron.core;
final case class CommittedEvent[+E <: Event](val streamType: String,
                                            val streamId: String,
                                            val commitRevision: Revision,
                                            val event: E) {
}