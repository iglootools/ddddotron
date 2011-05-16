package org.iglootools.ddddotron.testdomain

import org.iglootools.ddddotron._
import org.joda.time.DateTime

final case class AnotherCommandAccepted(val timestamp: DateTime = currentUtcDateTime) extends TestDomainEvent




