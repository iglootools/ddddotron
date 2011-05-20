package org.iglootools.ddddotron.tests

class HSQLDBRepositorySpec extends RepositorySpec {
 def locations = Array("classpath:/org/iglootools/ddddotron/infrastructure/eventstore/ddddotron-eventstore-context.xml",
    "classpath:/org/iglootools/ddddotron/infrastructure/eventstore/ddddotron-eventstore-hsqldb-context.xml")

  def databaseName = "HSQLDB"
}