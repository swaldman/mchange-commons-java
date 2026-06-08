organization := "com.mchange"

name := "mchange-commons-java"

version := "0.6.1-SNAPSHOT"

autoScalaLibrary := false // this is a pure Java library, don't depend on Scala

crossPaths := false //don't include _<scala-version> in artifact names

Compile / compile / javacOptions ++= Seq("-source","1.7","-target","1.7","-Xlint:deprecation"/*,"-Xlint:unchecked"*/)

//Compile / compile / javacOptions ++= Seq("-Xlint:all")

// some users want license files in source jar, go figure
// see https://github.com/swaldman/mchange-commons-java/issues/11
Compile / packageSrc / mappings ++= Seq(
  (baseDirectory.value / "LICENSE") -> "LICENSE",
  (baseDirectory.value / "LICENSE-LGPL") -> "LICENSE-LGPL",
  (baseDirectory.value / "LICENSE-EPL") -> "LICENSE-EPL"
)

Compile / doc / javacOptions ++= Seq("-source","1.7")

Compile / doc / javacOptions += "-Xdoclint:none"

libraryDependencies ++= Seq(
    "com.typesafe"             % "config"          % "1.3.0"   % "compile,optional",
    "log4j"                    % "log4j"           % "1.2.14+" % "compile,optional",
    "org.apache.logging.log4j" % "log4j-api"       % "2.17.1"  % "compile,optional",
    "org.apache.logging.log4j" % "log4j-core"      % "2.17.1"  % "compile,optional",
    "org.slf4j"                % "slf4j-api"       % "1.7.5"   % "compile,optional",
    "junit"                    % "junit"           % "4.1+"    % "test",
    "ch.qos.logback"           % "logback-classic" % "1.1.2"   % "test",
    "com.novocode"             % "junit-interface" % "0.11"    % "test"
);

publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (version.value.endsWith("-SNAPSHOT")) Some("central-snapshots" at centralSnapshots) else localStaging.value
}

Compile / packageBin / packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> "com.mchange.mchangecommonsjava")

/*
 * For some mysterious reasons, forking to test and setting java options on
 * the forked JVM is misbehaving. Workaround is to not fork at all, but
 * test with commands like
 *    $ export _JAVA_OPTIONS="-ea -Djava.util.logging.config.file=./src/test/resources/logging.properties"; sbt clean test; unset _JAVA_OPTIONS
 */ 
//scalacOptions += "-deprecation"
//fork in Test := true
//javaOptions in test += "-ea"

/*
 * Run test classes serially, not in parallel.
 *
 * Several of the security-related tests (notably the com.mchange.v2.naming.junit
 * ReferenceIndirector / ReferenceableUtils / JavaBeanReferenceable cases) configure
 * behavior by mutating global System properties (e.g. OBJECT_FACTORY_WHITELIST,
 * allowIndirectSerializationViaReference) and rely on the absence of values they did
 * not set. Since we do not fork (see above), all test classes share one JVM, and
 * sbt's default parallel test execution lets these classes clobber each other's
 * System-property state mid-test -- producing intermittent, ordering-dependent
 * failures (e.g. a pcfg-supplied whitelist being intersected to empty against a
 * sysprop whitelist set by a concurrently-running test). Serial execution gives each
 * test class the isolated global state it assumes.
 */
Test / parallelExecution := false

Test / logLevel := Level.Debug

/*
 * Arguments passed to the JUnit test runner (sbt's junit-interface).
 *
 *   -a  On a test failure, show the full stack trace and exception class for AssertionErrors.
 *       Without this, an assertion failure is reported only as its message, with no trace
 *       pointing at the failing line.
 *   -v  Verbose: log per-test "started"/"finished" events (rather than only a final summary),
 *       so you can see exactly which test classes and methods ran and in what order.
 *
 * See the junit-interface documentation for the full set of options:
 *   https://github.com/sbt/junit-interface
 */
testOptions += Tests.Argument(TestFrameworks.JUnit, "-a -v")

pomExtra := pomExtraForProjectName( name.value )

def pomExtraForProjectName( projectName : String ) = {
    <url>https://github.com/swaldman/{projectName}</url>
    <licenses>
      <license>
        <name>GNU Lesser General Public License, Version 2.1</name>
        <url>http://www.gnu.org/licenses/lgpl-2.1.html</url>
        <distribution>repo</distribution>
      </license>
      <license>
        <name>Eclipse Public License, Version 1.0</name>
        <url>http://www.eclipse.org/org/documents/epl-v10.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:swaldman/{projectName}.git</url>
      <connection>scm:git:git@github.com:swaldman/{projectName}</connection>
    </scm>
    <developers>
      <developer>
        <id>swaldman</id>
        <name>Steve Waldman</name>
        <email>swaldman@mchange.com</email>
      </developer>
    </developers>
}

enablePlugins(ParadoxPlugin)

val updateSite = taskKey[Unit]("Updates the project website on tickle")

updateSite := {
  import scala.sys.process._

  val dummy1 = (Compile / paradox).value // force a build of the site

  val localDir1 = target.value / "paradox" / "site" / "main"

  val local1 = localDir1.listFiles.map( _.getPath ).mkString(" ")
  val remote1 = s"tickle.mchange.com:/home/web/public/www.mchange.com/projects/${name.value}-versions/${version.value}/"
  s"rsync -avz ${local1} ${remote1}"!

  val dummy2 = (Compile / doc).value // force scaladocs

  val localDir2 = target.value / "api"
  val local2 = localDir2.listFiles.map( _.getPath ).mkString(" ")
  val remote2 = s"tickle.mchange.com:/home/web/public/www.mchange.com/projects/${name.value}-versions/${version.value}/apidocs"
  s"rsync -avz ${local2} ${remote2}"!
}
