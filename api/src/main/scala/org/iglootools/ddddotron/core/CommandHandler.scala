package org.iglootools.ddddotron.core

trait CommandHandler[C <: Command] {
  def handle(command: Command): Unit = handle(command.asInstanceOf[C])
  protected def doHandle(command: C)
  def supportedCommandType: Class[_]
}

abstract class CommandHandlerSupport[C <: Command](implicit m: scala.reflect.Manifest[C]) extends CommandHandler[C] {
  def supportedCommandType: Class[_] = m.erasure
}