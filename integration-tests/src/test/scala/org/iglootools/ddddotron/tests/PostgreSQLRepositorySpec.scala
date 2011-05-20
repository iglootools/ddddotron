package org.iglootools.ddddotron.tests

class PostgreSQLRepositorySpec extends RepositorySpec {
 def locations = Array("classpath:/org/iglootools/ddddotron/infrastructure/eventstore/ddddotron-eventstore-context.xml",
    "classpath:/org/iglootools/ddddotron/infrastructure/eventstore/ddddotron-eventstore-postgres-context.xml")

  def databaseName = "PostgreSQL"
}