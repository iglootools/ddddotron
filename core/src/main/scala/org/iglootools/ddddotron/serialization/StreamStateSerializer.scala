package org.iglootools.ddddotron.serialization

trait StreamStateSerializer {
  def serialize[S <: AnyRef](state: S): String
  def deserialize[S](data: String)(implicit mf: Manifest[S]): S
}