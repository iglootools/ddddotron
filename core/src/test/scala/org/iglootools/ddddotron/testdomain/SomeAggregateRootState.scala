package org.iglootools.ddddotron.testdomain

import org.iglootools.ddddotron._
import org.joda.time.DateTime


final case class SomeAggregateRootState(val event1Applied: Boolean=false,
                                        val event2Applied: Boolean=false,
                                        val someCommandAccepted: Boolean=false,
                                        val anotherCommandAccepted: Boolean=false)



