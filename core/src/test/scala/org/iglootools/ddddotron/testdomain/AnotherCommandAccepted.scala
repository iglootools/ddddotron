package org.iglootools.ddddotron.testdomain

import org.iglootools.ddddotron._
import org.joda.time.DateTime

final case class AnotherCommandAccepted(val eventUtcDate: DateTime = currentUtcDateTime) extends TestDomainEvent




