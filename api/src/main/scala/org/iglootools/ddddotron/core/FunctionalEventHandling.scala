package org.iglootools.ddddotron.core

import org.iglootools.commons.scala.handlerregistry.EventHandler

trait FunctionalEventHandling {
  protected[this] def handler[A, B](callback: A => B) = EventHandler.handler(callback)
  implicit protected[this] def handlerToPartialFunction[A, B](handler: EventHandler[A, B])(implicit m: Manifest[A]) = EventHandler.handlerToPartialFunction(handler)
}