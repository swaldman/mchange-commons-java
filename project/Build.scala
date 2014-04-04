import sbt._

object MchangeCommonsJavaBuild extends Build {

  val nexus = "https://oss.sonatype.org/"
  val nexusSnapshots = nexus + "content/repositories/snapshots";
  val nexusReleases = nexus + "service/local/staging/deploy/maven2";

  val projectName = "mchange-commons-java"

  val mySettings = Seq( 
    Keys.organization := "com.mchange",
    Keys.name := projectName, 
    Keys.version := "0.2.7", 

    //Keys.scalaVersion := "2.10.1",
    //Keys.scalaVersion := "2.9.2",

    Keys.autoScalaLibrary := false, // this is a pure Java library, don't depend on Scala
    Keys.crossPaths := false,       //don't include _<scala-version> in artifact names

    Keys.javacOptions in (Compile, Keys.compile) ++= Seq("-source","1.6","-target","1.6"),
    Keys.javacOptions in (Compile, Keys.doc) ++= Seq("-source","1.6"),

    Keys.publishTo <<= Keys.version { 
      (v: String) => {
	if (v.trim.endsWith("SNAPSHOT"))
	  Some("snapshots" at nexusSnapshots )
	else
	  Some("releases"  at nexusReleases )
      }
    },
    Keys.resolvers += ("snapshots" at nexusSnapshots ),

    /*
     * For some mysterious reasons, forking to test and setting java options on
     * the forked JVM is misbehaving. Workaround is to not fork at all, but
     * test with commands like
     *    $ export _JAVA_OPTIONS="-ea -Djava.util.logging.config.file=./src/test/resources/logging.properties"; sbt clean test; unset _JAVA_OPTIONS
     */ 
    //Keys.scalacOptions += "-deprecation",
    //Keys.fork in Test := true,
    //Keys.javaOptions in test += "-ea",
    Keys.logLevel in Test := Level.Debug,
    Keys.testOptions += Tests.Argument(TestFrameworks.JUnit, "-a -v"),

    Keys.pomExtra := pomExtraXml
  );

  val dependencies = Seq(
    "com.typesafe" % "config" % "1.0.0" % "compile,optional",
    "log4j" % "log4j" % "1.2.14+" % "compile,optional",
    "org.slf4j" % "slf4j-api" % "1.7.5+" % "compile,optional",
    "junit" % "junit" % "4.1+" % "test",
    "com.novocode" % "junit-interface" % "0.10-M3" % "test" 
  );

  override lazy val settings = super.settings ++ mySettings;

  lazy val mainProject = Project(
    id = projectName,
    base = file("."),
    settings = Project.defaultSettings ++ (Keys.libraryDependencies ++= dependencies)
  ); 

  val pomExtraXml = (
      <url>https://github.com/swaldman/mchange-commons-java</url>
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
       <url>git@github.com:swaldman/mchange-commons-java.git</url>
       <connection>scm:git:git@github.com:swaldman/mchange-commons-java.git</connection>
     </scm>
     <developers>
       <developer>
         <id>swaldman</id>
         <name>Steve Waldman</name>
         <email>swaldman@mchange.com</email>
       </developer>
     </developers>
  );
}

