package org.iglootools.ddddotron.core

import org.iglootools.commons.scala.handlerregistry.{HandlerTypeRegistry}

class CompositeCommandHandler[C <: Command](val commandHandlers: CommandHandler[C]*) {
  require(commandHandlers != null, "commandHandlers is required")
  val handlerRegistry = new HandlerTypeRegistry[CommandHandler[C]](List(commandHandlers : _*), { h => h.supportedCommandType} )

  def handle(command: Command) = {
    require(command != null, "command is required")
    handlerRegistry.handlerFor(command.getClass).handle(command)
  }

}