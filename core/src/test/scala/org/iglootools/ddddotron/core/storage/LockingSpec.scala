package org.iglootools.ddddotron.core.storage

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.testdomain._
import org.scalatest.Spec

@RunWith(classOf[JUnitRunner])
class LockingSpec extends Spec with ShouldMatchers {

  describe("expected revisions") {
    it("should start numbering at 1 when no revision") {
      Locking.expectedRevisions(None, 3) should be === List(1,2,3)
    }
    it("should start numbering after revision") {
      Locking.expectedRevisions(Some(1), 3) should be === List(2,3,4)
    }
    it("should return Nil when number of events is 0") {
      Locking.expectedRevisions(Some(1), 0) should be === Nil
    }
  }

}