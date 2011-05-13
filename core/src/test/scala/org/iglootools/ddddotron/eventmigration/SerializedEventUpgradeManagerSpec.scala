package org.iglootools.ddddotron.eventmigration

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.iglootools.ddddotron._
import org.iglootools.ddddotron.testdomain._
import org.scalatest.Spec


trait UpgraderForMyEvent extends SerializedEventUpgrader {
  def eventType: String = "MyEvent"

  def assumeFormat(expected: String, actual: String) {
    assume(expected == actual, "Expected: %s, Actual: %s".format(expected, actual))
  }
}

object FromVersion1 extends UpgraderForMyEvent {
  def doConvertData(data: String): String = {
    assumeFormat("v1", data)
    "v2"
  }
  def fromVersion: Int = 1
}

object FromVersion2 extends UpgraderForMyEvent {
  def doConvertData(data: String): String = {
    assumeFormat("v2", data)
    "v3"
  }
  def fromVersion: Int = 2
}

object FromVersion3 extends UpgraderForMyEvent {
  def doConvertData(data: String): String = {
    assumeFormat("v3", data)
    "v4"
  }
  def fromVersion: Int = 3
}

@RunWith(classOf[JUnitRunner])
class SerializedEventUpgradeManagerSpec extends Spec with ShouldMatchers {

  describe("SerializedEventUpgradeManager") {

    it("should use all applying upgraders in the right order") {
      val upgradeManager = new DefaultSerializedEventUpgradeManager(List(FromVersion2, FromVersion1))
      upgradeManager.upgradeToMostRecentVersion(SerializedEvent("MyEvent", 1, "v1")) should be === SerializedEvent("MyEvent", 3, "v3")
    }

    it("should produce exception if versions do not match between the upgraders and the event") {
      val upgradeManager = new DefaultSerializedEventUpgradeManager(List(FromVersion1, FromVersion3))
      evaluating { upgradeManager.upgradeToMostRecentVersion(SerializedEvent("MyEvent", 1, "v1")) } should produce[AssertionError]
    }
  }
}