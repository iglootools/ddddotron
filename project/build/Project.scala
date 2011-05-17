import java.io.File
import sbt._

class Project(info: ProjectInfo) extends ParentProject(info) with IdeaProject  {
  lazy val iglootoolsRepository = "Iglootools Releases Repository" at "http://www.iglootools.org/artifactory/iglootools-release"

  override def managedStyle = ManagedStyle.Maven
  //val publishTo = "Iglootools" at "http://www.iglootools.org/artifactory/iglootools-release-local"
  //Credentials(Path.userHome / ".ivy2" / ".credentials", log)

  lazy val api = project("api", "ddddotron-api", new Api(_))
  lazy val eventstore = project("eventstore", "ddddotron-eventstore", new EventStore(_), api)
  lazy val integrationTests = project("integration-tests", "integration-tests", new IntegrationTests(_), api)

  object Dependencies {
    val SpringFrameworkVersion = "3.0.5.RELEASE"
    val JunitVersion = "4.8.2"
    val LogbackVersion = "0.9.27"
    val JunitInterfaceVersion = "0.6"
    val LiftJsonVersion = "2.3"
    val CamelVersion = "2.7.1"
    val ActiveMqVersion = "5.5.0"

    object Test {
      lazy val junit = "junit" % "junit" % Dependencies.JunitVersion % "test" withSources()
      //lazy val junitInterface = "com.novocode" % "junit-interface" % Dependencies.JunitInterfaceVersion % "test->default"
      lazy val scalaTest = "org.scalatest" % "scalatest" % "1.2"  % "test" withSources()
      lazy val mockito = "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources()
      lazy val logbackTest = "ch.qos.logback" % "logback-classic" % Dependencies.LogbackVersion % "test" // withSources()
      lazy val springTest = "org.springframework" % "spring-test" % Dependencies.SpringFrameworkVersion % "test" withSources()
    }

    def ivyXML =
      <dependencies>
          <exclude org="commons-logging" /> <!-- we want to use jcl-over-slf4j instead -->
          <override org="org.springframework" rev="3.0.5.RELEASE" />
      </dependencies>
  }

  class Api(info: ProjectInfo) extends DefaultProject(info) with IdeaProject {
    override def ivyXML = Dependencies.ivyXML
    val grizzled = "org.clapper" %% "grizzled-slf4j" % "0.4"

    // commons
    lazy val slf4jApi = "org.slf4j" % "slf4j-api" % "1.6.1" withSources()
    lazy val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % "1.6.1" withSources()

    lazy val guava = "com.google.guava" % "guava" % "r08" withSources()
    lazy val jodaTime = "joda-time" % "joda-time" % "1.6.2" withSources()
    lazy val scalaTime = "org.scala-tools.time" %% "time" % "0.3" // withSources()
    lazy val scalaj_collection = "org.scalaj" % "scalaj-collection_2.8.0" % "1.0"
    lazy val iglootoolsCommons = "org.iglootools.commons" %% "iglootools-commons-scala" % "0.1"


    // test
    lazy val junit = Dependencies.Test.junit
    lazy val scalaTest = Dependencies.Test.scalaTest
    lazy val mockito = Dependencies.Test.mockito
    lazy val logbackTest = Dependencies.Test.logbackTest
    lazy val springTest = Dependencies.Test.springTest
  }

  class EventStore(info: ProjectInfo) extends DefaultProject(info) with IdeaProject {
    override def ivyXML = Dependencies.ivyXML

    lazy val liftJson = "net.liftweb" %% "lift-json" % Dependencies.LiftJsonVersion withSources()
    lazy val liftJsonExt = "net.liftweb" %% "lift-json-ext" % Dependencies.LiftJsonVersion withSources()
    lazy val springJdbc = "org.springframework" % "spring-jdbc" % Dependencies.SpringFrameworkVersion withSources()
    lazy val springContextSupport = "org.springframework" % "spring-context-support" % Dependencies.SpringFrameworkVersion withSources()
    lazy val hsqldb = "org.hsqldb" % "hsqldb" % "2.0.0" //withSources()
    lazy val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.2"

    // test
    lazy val junit = Dependencies.Test.junit
    lazy val scalaTest = Dependencies.Test.scalaTest
    lazy val mockito = Dependencies.Test.mockito
    lazy val logbackTest = Dependencies.Test.logbackTest
    lazy val springTest = Dependencies.Test.springTest
  }

  class IntegrationTests(info: ProjectInfo) extends DefaultProject(info) with IdeaProject {
    override def ivyXML = Dependencies.ivyXML
    override def testOptions = TestFilter(s => true) :: super.testOptions.toList

    // test dependencies
    lazy val junit = Dependencies.Test.junit
    lazy val scalaTest = Dependencies.Test.scalaTest
    lazy val logback = Dependencies.Test.logbackTest
  }

}
